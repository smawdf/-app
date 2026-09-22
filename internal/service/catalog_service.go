package service

import (
	"errors"
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"

	"orderdisk-server/internal/model"
	"orderdisk-server/internal/ws"
)

// MenuItemInput 菜品新增/编辑入参
type MenuItemInput struct {
	Name        string  `json:"name"`
	Description string  `json:"description"`
	ImageURL    string  `json:"image_url"`
	CategoryID  string  `json:"category_id"`
	Price       float64 `json:"price"`
	IsAvailable *bool   `json:"is_available"`
}

// Me 返回当前用户 + 配对 + 小店，供前端启动时一次性拉取档案
func (s *AppService) Me(userID string) (*model.User, *model.Pair, *model.Shop, error) {
	var user model.User
	if err := s.db.Where("id = ?", userID).First(&user).Error; err != nil {
		return nil, nil, nil, errors.New("用户不存在")
	}

	var pair model.Pair
	var shop model.Shop
	if user.PairID != "" {
		if err := s.db.Where("id = ?", user.PairID).First(&pair).Error; err != nil {
			return &user, nil, nil, nil
		}
		// 小店可能尚未初始化，容错处理
		if err := s.db.Where("pair_id = ?", pair.ID).First(&shop).Error; err != nil {
			return &user, &pair, nil, nil
		}
		return &user, &pair, &shop, nil
	}
	return &user, nil, nil, nil
}

// GetShop 获取情侣小店信息
func (s *AppService) GetShop(pairID string) (*model.Shop, error) {
	var shop model.Shop
	if err := s.db.Where("pair_id = ?", pairID).First(&shop).Error; err != nil {
		return nil, errors.New("小店不存在")
	}
	return &shop, nil
}

// ListMenu 拉取该情侣小店的完整菜单
func (s *AppService) ListMenu(pairID string) ([]model.MenuItem, error) {
	var items []model.MenuItem
	err := s.db.Where("pair_id = ?", pairID).
		Order("created_at desc").
		Find(&items).Error
	return items, err
}

// CreateMenuItem 饲养员上新菜品
func (s *AppService) CreateMenuItem(pairID string, in MenuItemInput) (*model.MenuItem, error) {
	if in.Name == "" {
		return nil, errors.New("菜名不能为空")
	}
	if in.Price <= 0 {
		return nil, errors.New("菜品价格必须大于 0")
	}

	available := true
	if in.IsAvailable != nil {
		available = *in.IsAvailable
	}

	item := &model.MenuItem{
		ID:          uuid.New().String(),
		PairID:      pairID,
		CategoryID:  in.CategoryID,
		Name:        in.Name,
		Description: in.Description,
		Price:       in.Price,
		ImageURL:    in.ImageURL,
		IsAvailable: available,
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}
	if err := s.db.Create(item).Error; err != nil {
		return nil, err
	}

	ws.GlobalHub.BroadcastToPair(pairID, "menu_updated", item)
	return item, nil
}

// UpdateMenuItem 饲养员编辑菜品
func (s *AppService) UpdateMenuItem(pairID, itemID string, in MenuItemInput) (*model.MenuItem, error) {
	var item model.MenuItem
	if err := s.db.Where("id = ? AND pair_id = ?", itemID, pairID).First(&item).Error; err != nil {
		return nil, errors.New("菜品不存在")
	}

	if in.Name != "" {
		item.Name = in.Name
	}
	if in.Price > 0 {
		item.Price = in.Price
	}
	item.Description = in.Description
	item.ImageURL = in.ImageURL
	if in.CategoryID != "" {
		item.CategoryID = in.CategoryID
	}
	if in.IsAvailable != nil {
		item.IsAvailable = *in.IsAvailable
	}
	item.UpdatedAt = time.Now()

	if err := s.db.Save(&item).Error; err != nil {
		return nil, err
	}

	ws.GlobalHub.BroadcastToPair(pairID, "menu_updated", item)
	return &item, nil
}

// DeleteMenuItem 饲养员下架菜品
func (s *AppService) DeleteMenuItem(pairID, itemID string) error {
	var item model.MenuItem
	if err := s.db.Where("id = ? AND pair_id = ?", itemID, pairID).First(&item).Error; err != nil {
		return errors.New("菜品不存在")
	}
	if err := s.db.Delete(&item).Error; err != nil {
		return err
	}

	ws.GlobalHub.BroadcastToPair(pairID, "menu_updated", map[string]string{
		"deleted_id": itemID,
	})
	return nil
}

// ListOrders 拉取该情侣的全部订单（含明细）
func (s *AppService) ListOrders(pairID string) ([]model.Order, error) {
	var orders []model.Order
	err := s.db.Preload("Items").
		Where("pair_id = ?", pairID).
		Order("created_at desc").
		Find(&orders).Error
	return orders, err
}

// GetOrder 拉取单个订单详情
func (s *AppService) GetOrder(pairID, orderID string) (*model.Order, error) {
	var order model.Order
	if err := s.db.Preload("Items").
		Where("id = ? AND pair_id = ?", orderID, pairID).
		First(&order).Error; err != nil {
		return nil, errors.New("订单不存在")
	}
	return &order, nil
}

// ListCandyTransactions 糖糖币收支流水
func (s *AppService) ListCandyTransactions(pairID string) ([]model.CandyTransaction, error) {
	var list []model.CandyTransaction
	err := s.db.Where("pair_id = ?", pairID).
		Order("created_at desc").
		Limit(100).
		Find(&list).Error
	return list, err
}

// SeedDefaultMenu 情侣配对成功时，自动为小店预置一批家常招牌菜
func SeedDefaultMenu(tx *gorm.DB, pairID string) error {
	now := time.Now()
	dishes := []struct {
		name  string
		tag   string
		price float64
		emoji string
	}{
		{"蜜汁可乐小鸡翅", "饲养员招牌拿手菜", 18, "🍗"},
		{"草莓生巧舒芙蕾", "吃货点名必吃榜 TOP 1", 22, "🥞"},
		{"暖胃浓汤番茄牛腩", "砂锅慢火现炖", 36, "🍲"},
		{"法式巴斯克乳酪蛋糕", "浓郁芝士爆浆", 15, "🍰"},
		{"日式肥牛寿喜烧锅", "热气腾腾双人份", 42, "🥘"},
		{"鲜榨白桃乌龙冰茶", "解腻清爽夏日特饮", 12, "🍑"},
	}

	items := make([]model.MenuItem, 0, len(dishes))
	for _, d := range dishes {
		items = append(items, model.MenuItem{
			ID:          uuid.New().String(),
			PairID:      pairID,
			Name:        d.name,
			Description: d.tag,
			Price:       d.price,
			ImageURL:    d.emoji, // 暂用 emoji 占位图，后续可替换为真实图片 URL
			IsAvailable: true,
			CreatedAt:   now,
			UpdatedAt:   now,
		})
	}
	return tx.Create(&items).Error
}
