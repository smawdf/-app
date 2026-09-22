package service

import (
	"crypto/rand"
	"errors"
	"fmt"
	"math"
	"math/big"
	"time"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"

	"orderdisk-server/internal/model"
	"orderdisk-server/internal/ws"
)

type AppService struct {
	db *gorm.DB
}

func NewAppService(db *gorm.DB) *AppService {
	return &AppService{db: db}
}

// -------------------------------------------------------------
// 1. 用户与情侣配对服务
// -------------------------------------------------------------

func (s *AppService) Register(username, password, nickname, role string) (*model.User, error) {
	var exist model.User
	if err := s.db.Where("username = ?", username).First(&exist).Error; err == nil {
		return nil, errors.New("用户名已被占用")
	}

	hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		return nil, err
	}

	user := &model.User{
		ID:        uuid.New().String(),
		Username:  username,
		Password:  string(hashed),
		Nickname:  nickname,
		Role:      role,
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	if err := s.db.Create(user).Error; err != nil {
		return nil, err
	}
	return user, nil
}

func (s *AppService) Login(username, password string) (*model.User, error) {
	var user model.User
	if err := s.db.Where("username = ?", username).First(&user).Error; err != nil {
		return nil, errors.New("账号不存在或密码错误")
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password)); err != nil {
		return nil, errors.New("账号不存在或密码错误")
	}

	return &user, nil
}

// GenerateInviteCode 生成 6 位大写随机配对邀请码
func generateInviteCode() string {
	const letters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
	result := make([]byte, 6)
	for i := range result {
		num, _ := rand.Int(rand.Reader, big.NewInt(int64(len(letters))))
		result[i] = letters[num.Int64()]
	}
	return string(result)
}

// CreatePair 生成情侣配对邀请码
func (s *AppService) CreatePair(userID, role string) (*model.Pair, error) {
	pair := &model.Pair{
		ID:         uuid.New().String(),
		InviteCode: generateInviteCode(),
		CandyCoins: 66, // 初值 66 糖币
		CreatedAt:  time.Now(),
		UpdatedAt:  time.Now(),
	}
	if role == model.RoleCaretaker {
		pair.CaretakerID = userID
	} else {
		pair.EaterID = userID
	}

	err := s.db.Transaction(func(tx *gorm.DB) error {
		if err := tx.Create(pair).Error; err != nil {
			return err
		}
		// 初始化该情侣的小店
		shop := &model.Shop{
			ID:           uuid.New().String(),
			PairID:       pair.ID,
			Name:         "我们的小家餐厅 💕",
			Announcement: "今日也是热气腾腾的幸福一天~",
			CreatedAt:    time.Now(),
			UpdatedAt:    time.Now(),
		}
		if err := tx.Create(shop).Error; err != nil {
			return err
		}
		// 预置一批家常招牌菜，让新配对的小店开箱即有菜单
		if err := SeedDefaultMenu(tx, pair.ID); err != nil {
			return err
		}
		// 更新用户的 PairID
		return tx.Model(&model.User{}).Where("id = ?", userID).Update("pair_id", pair.ID).Error
	})

	if err != nil {
		return nil, err
	}
	return pair, nil
}

// JoinPair 输入 6 位邀请码绑定伴侣
func (s *AppService) JoinPair(userID, role, inviteCode string) (*model.Pair, error) {
	var pair model.Pair
	if err := s.db.Where("invite_code = ?", inviteCode).First(&pair).Error; err != nil {
		return nil, errors.New("邀请码无效或已失效")
	}

	// 互补角色自动分配或填充
	if role == model.RoleCaretaker {
		if pair.CaretakerID != "" && pair.CaretakerID != userID {
			return nil, errors.New("该小店已经有饲养员掌勺了哦！")
		}
		pair.CaretakerID = userID
	} else {
		if pair.EaterID != "" && pair.EaterID != userID {
			return nil, errors.New("该小店已经有首席吃货啦！")
		}
		pair.EaterID = userID
	}

	now := time.Now()
	pair.AnniversaryAt = &now

	err := s.db.Transaction(func(tx *gorm.DB) error {
		if err := tx.Save(&pair).Error; err != nil {
			return err
		}
		return tx.Model(&model.User{}).Where("id = ?", userID).Update("pair_id", pair.ID).Error
	})

	if err != nil {
		return nil, err
	}

	// WebSocket 实时推送配对成功事件
	ws.GlobalHub.BroadcastToPair(pair.ID, "pair_joined", pair)

	return &pair, nil
}

// -------------------------------------------------------------
// 2. 糖糖币业务服务（事务硬隔离）
// -------------------------------------------------------------

// RechargeCandyCoins 饲养员专属：给小铺充值/撒糖
func (s *AppService) RechargeCandyCoins(pairID, caretakerID string, amount int, reason string) (*model.Pair, error) {
	if amount <= 0 {
		return nil, errors.New("充值糖币数量必须大于0")
	}

	var pair model.Pair
	err := s.db.Transaction(func(tx *gorm.DB) error {
		if err := tx.Where("id = ?", pairID).First(&pair).Error; err != nil {
			return err
		}
		if pair.CaretakerID != caretakerID {
			return errors.New("CARETAKER_ROLE_REQUIRED")
		}

		pair.CandyCoins += amount
		if err := tx.Save(&pair).Error; err != nil {
			return err
		}

		// 记录流水
		trans := &model.CandyTransaction{
			ID:          uuid.New().String(),
			PairID:      pairID,
			Amount:      amount,
			Balance:     pair.CandyCoins,
			ActionType:  "recharge",
			Description: fmt.Sprintf("饲养员投喂撒糖: %s", reason),
			CreatedAt:   time.Now(),
		}
		return tx.Create(trans).Error
	})

	if err != nil {
		return nil, err
	}

	// 广播给伴侣：糖币增加了！
	ws.GlobalHub.BroadcastToPair(pairID, "candy_changed", map[string]interface{}{
		"candy_coins": pair.CandyCoins,
		"delta":       amount,
		"reason":      reason,
	})

	return &pair, nil
}

// -------------------------------------------------------------
// 3. 点菜与订单状态机服务（强事务与双向推送）
// -------------------------------------------------------------

type CreateOrderItemDTO struct {
	MenuID    string  `json:"menu_id"`
	Name      string  `json:"name"`
	ImageURL  string  `json:"image_url"`
	UnitPrice float64 `json:"unit_price"`
	Quantity  int     `json:"quantity"`
}

type CreateOrderDTO struct {
	BuyerNote string               `json:"buyer_note"`
	Items     []CreateOrderItemDTO `json:"items"`
}

// SubmitOrder 吃货专属：选菜下单并扣减糖糖币
func (s *AppService) SubmitOrder(eaterID, pairID string, dto CreateOrderDTO) (*model.Order, error) {
	if len(dto.Items) == 0 {
		return nil, errors.New("购物车是空的，无法下单")
	}

	var total float64
	for _, it := range dto.Items {
		total += it.UnitPrice * float64(it.Quantity)
	}
	// 糖币消耗：向上取整，如 18.5 元 = 19 糖币
	coinsNeeded := int(math.Ceil(total))
	if coinsNeeded < 1 {
		coinsNeeded = 1
	}

	var buyer model.User
	if err := s.db.Where("id = ?", eaterID).First(&buyer).Error; err != nil {
		return nil, err
	}

	orderID := uuid.New().String()
	order := &model.Order{
		ID:              orderID,
		PairID:          pairID,
		BuyerID:         eaterID,
		BuyerName:       buyer.Nickname,
		BuyerAvatarURL:  buyer.AvatarURL,
		Status:          model.StatusSubmitted,
		BuyerNote:       dto.BuyerNote,
		Subtotal:        total,
		TotalPrice:      total,
		CandyCoinsSpent: coinsNeeded,
		CreatedAt:       time.Now(),
		UpdatedAt:       time.Now(),
	}

	for _, itemDTO := range dto.Items {
		order.Items = append(order.Items, model.OrderItem{
			ID:        uuid.New().String(),
			OrderID:   orderID,
			MenuID:    itemDTO.MenuID,
			Name:      itemDTO.Name,
			ImageURL:  itemDTO.ImageURL,
			UnitPrice: itemDTO.UnitPrice,
			Quantity:  itemDTO.Quantity,
			Subtotal:  itemDTO.UnitPrice * float64(itemDTO.Quantity),
			CreatedAt: time.Now(),
		})
	}

	// 核心：强事务扣除糖币并记录订单，防止并发超扣
	err := s.db.Transaction(func(tx *gorm.DB) error {
		var pair model.Pair
		if err := tx.Where("id = ?", pairID).First(&pair).Error; err != nil {
			return err
		}

		if pair.CandyCoins < coinsNeeded {
			return fmt.Errorf("NOT_ENOUGH_CANDY_COINS: 还需要 %d 糖币，当前余额 %d", coinsNeeded, pair.CandyCoins)
		}

		// 扣除糖币
		pair.CandyCoins -= coinsNeeded
		if err := tx.Save(&pair).Error; err != nil {
			return err
		}

		// 插入订单主表和明细
		if err := tx.Create(order).Error; err != nil {
			return err
		}

		// 累计菜品销量
		for _, it := range dto.Items {
			if it.MenuID != "" {
				tx.Model(&model.MenuItem{}).Where("id = ?", it.MenuID).
					UpdateColumn("sales_count", gorm.Expr("sales_count + ?", it.Quantity))
			}
		}

		// 写入流水
		trans := &model.CandyTransaction{
			ID:          uuid.New().String(),
			PairID:      pairID,
			Amount:      -coinsNeeded,
			Balance:     pair.CandyCoins,
			ActionType:  "spend",
			Description: fmt.Sprintf("吃货点菜支出 (订单: %s)", orderID),
			ReferenceID: orderID,
			CreatedAt:   time.Now(),
		}
		return tx.Create(trans).Error
	})

	if err != nil {
		return nil, err
	}

	// 广播给饲养员：收到新订单！叮咚！
	ws.GlobalHub.BroadcastToPair(pairID, "order_created", order)

	return order, nil
}

// AdvanceOrderStatus 饲养员专属：推进做饭状态机 (confirmed -> preparing -> delivering -> completed)
func (s *AppService) AdvanceOrderStatus(caretakerID, pairID, orderID, newStatus string) (*model.Order, error) {
	validStatuses := map[string]bool{
		model.StatusConfirmed:  true,
		model.StatusPreparing:  true,
		model.StatusDelivering: true,
		model.StatusCompleted:  true,
	}
	if !validStatuses[newStatus] {
		return nil, errors.New("无效的做饭状态目标")
	}

	var order model.Order
	if err := s.db.Preload("Items").Where("id = ? AND pair_id = ?", orderID, pairID).First(&order).Error; err != nil {
		return nil, errors.New("订单不存在")
	}

	order.Status = newStatus
	order.UpdatedAt = time.Now()
	if err := s.db.Save(&order).Error; err != nil {
		return nil, err
	}

	// 实时广播状态变动给吃货（例如：饲养员正在炖煮、快上桌啦！）
	ws.GlobalHub.BroadcastToPair(pairID, "order_updated", order)

	return &order, nil
}

// CancelOrder 取消订单（支持全自动原路退回糖币）
func (s *AppService) CancelOrder(userID, pairID, orderID string) error {
	return s.db.Transaction(func(tx *gorm.DB) error {
		var order model.Order
		if err := tx.Where("id = ? AND pair_id = ?", orderID, pairID).First(&order).Error; err != nil {
			return errors.New("订单不存在")
		}
		if order.Status == model.StatusCompleted || order.Status == model.StatusCancelled {
			return errors.New("当前状态不可取消")
		}

		order.Status = model.StatusCancelled
		order.UpdatedAt = time.Now()
		if err := tx.Save(&order).Error; err != nil {
			return err
		}

		// 退回糖币
		var pair model.Pair
		if err := tx.Where("id = ?", pairID).First(&pair).Error; err != nil {
			return err
		}
		pair.CandyCoins += order.CandyCoinsSpent
		if err := tx.Save(&pair).Error; err != nil {
			return err
		}

		// 写入退款流水
		trans := &model.CandyTransaction{
			ID:          uuid.New().String(),
			PairID:      pairID,
			Amount:      order.CandyCoinsSpent,
			Balance:     pair.CandyCoins,
			ActionType:  "refund",
			Description: fmt.Sprintf("订单取消退还糖币: %s", orderID),
			ReferenceID: orderID,
			CreatedAt:   time.Now(),
		}
		if err := tx.Create(trans).Error; err != nil {
			return err
		}

		ws.GlobalHub.BroadcastToPair(pairID, "order_cancelled", map[string]interface{}{
			"order_id":       orderID,
			"refunded_coins": order.CandyCoinsSpent,
			"candy_coins":    pair.CandyCoins,
		})

		return nil
	})
}
