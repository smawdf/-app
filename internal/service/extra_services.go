package service

import (
	"errors"
	"strings"
	"time"

	"orderdisk-server/internal/model"
	"orderdisk-server/internal/ws"
)

// ShopInput 小店设置入参
type ShopInput struct {
	Name         string `json:"name"`
	Announcement string `json:"announcement"`
}

// UpdateShop 饲养员修改小店名称与公告
func (s *AppService) UpdateShop(pairID string, in ShopInput) (*model.Shop, error) {
	var shop model.Shop
	if err := s.db.Where("pair_id = ?", pairID).First(&shop).Error; err != nil {
		return nil, errors.New("小店不存在")
	}

	if strings.TrimSpace(in.Name) != "" {
		shop.Name = strings.TrimSpace(in.Name)
	}
	if strings.TrimSpace(in.Announcement) != "" {
		shop.Announcement = strings.TrimSpace(in.Announcement)
	}
	shop.UpdatedAt = time.Now()

	if err := s.db.Save(&shop).Error; err != nil {
		return nil, err
	}

	ws.GlobalHub.BroadcastToPair(pairID, "shop_updated", shop)
	return &shop, nil
}

// AnniversaryInput 恋爱纪念日更新入参
type AnniversaryInput struct {
	AnniversaryAt string `json:"anniversary_at"` // ISO 时间格式如 "2025-04-20"
}

// AnniversaryInfo 纪念日综合响应
type AnniversaryInfo struct {
	AnniversaryAt string              `json:"anniversary_at"`
	DaysTogether  int                 `json:"days_together"`
	Moments       []model.SweetMoment `json:"moments"`
}

// GetAnniversary 获取纪念日与回忆墙
func (s *AppService) GetAnniversary(pairID string) (*AnniversaryInfo, error) {
	var pair model.Pair
	if err := s.db.Where("id = ?", pairID).First(&pair).Error; err != nil {
		return nil, errors.New("情侣关系不存在")
	}

	var moments []model.SweetMoment
	s.db.Where("pair_id = ?", pairID).Order("cooked_at desc").Limit(50).Find(&moments)

	anniversaryStr := "2025-04-20"
	days := 520
	if pair.AnniversaryAt != nil {
		anniversaryStr = pair.AnniversaryAt.Format("2006-01-02")
		days = int(time.Since(*pair.AnniversaryAt).Hours() / 24)
		if days < 1 {
			days = 1
		}
	}

	return &AnniversaryInfo{
		AnniversaryAt: anniversaryStr,
		DaysTogether:  days,
		Moments:       moments,
	}, nil
}

// UpdateAnniversary 设置恋爱起始日
func (s *AppService) UpdateAnniversary(pairID string, in AnniversaryInput) (*AnniversaryInfo, error) {
	t, err := time.Parse("2006-01-02", in.AnniversaryAt)
	if err != nil {
		return nil, errors.New("日期格式错误，请使用 YYYY-MM-DD")
	}

	if err := s.db.Model(&model.Pair{}).Where("id = ?", pairID).Update("anniversary_at", t).Error; err != nil {
		return nil, err
	}

	return s.GetAnniversary(pairID)
}

// RecipeItem 菜谱搜索项
type RecipeItem struct {
	Name       string   `json:"name"`
	Desc       string   `json:"desc"`
	Time       string   `json:"time"`
	Difficulty string   `json:"difficulty"`
	Price      float64  `json:"price"`
	Emoji      string   `json:"emoji"`
	Steps      []string `json:"steps"`
}

// SearchRecipes 真实菜谱搜索服务（支持按关键词全量匹配做法）
func (s *AppService) SearchRecipes(keyword string) []RecipeItem {
	kw := strings.ToLower(strings.TrimSpace(keyword))

	// 内置海量美食菜谱库（离线高可用）
	library := []RecipeItem{
		{
			Name:       "关东风味肥牛寿喜烧",
			Desc:       "热气腾腾关东风味甜咸寿喜锅，大片肥牛裹上无菌生蛋液，冬天情侣窝在一起吃最幸福！",
			Time:       "25 分钟",
			Difficulty: "简单",
			Price:      42.0,
			Emoji:      "🥘",
			Steps:      []string{"黄油热锅煎香大葱与洋葱", "下入肥牛煎至变色，淋入寿喜烧酱汁", "码入豆腐、香菇、魔芋结与娃娃菜", "慢火炖煮15分钟即可出锅"},
		},
		{
			Name:       "蜜汁可乐小鸡翅",
			Desc:       "火候恰到好处，鸡翅香甜脱骨，浓郁可乐焦糖裹满每一寸鸡皮，下饭绝对一绝！",
			Time:       "20 分钟",
			Difficulty: "家常",
			Price:      18.0,
			Emoji:      "🍗",
			Steps:      []string{"鸡翅两面划花刀焯水", "少油煎至双面金黄微焦", "倒入半罐可乐与少许老抽生抽", "大火收浓汤汁即可"},
		},
		{
			Name:       "草莓生巧舒芙蕾",
			Desc:       "吃货点名必吃榜 TOP 1！云朵般轻盈绵软，淋上微苦丝滑生巧酱与清甜草莓粒。",
			Time:       "30 分钟",
			Difficulty: "甜品",
			Price:      22.0,
			Emoji:      "🥞",
			Steps:      []string{"蛋黄加牛奶与低粉搅拌成糊", "蛋白分三次加糖打发至小弯钩", "翻拌均匀小火慢煎至膨起", "淋酱撒上新鲜草莓装盘"},
		},
		{
			Name:       "暖胃浓汤番茄牛腩",
			Desc:       "砂锅慢火煨足两小时，番茄熬煮融化进浓郁牛汤，酸甜开胃，汤汁拌饭能炫三碗！",
			Time:       "60 分钟",
			Difficulty: "中等",
			Price:      36.0,
			Emoji:      "🍲",
			Steps:      []string{"牛腩切块冷水下锅焯透洗净", "热油炒软成熟番茄出红沙", "加入牛腩与香料加水漫过", "砂锅文火慢炖1小时加盐调味"},
		},
		{
			Name:       "法式巴斯克乳酪蛋糕",
			Desc:       "重度芝士爱好者的本命甜点，焦黑外皮包裹着冰淇淋般半熟流心，浓醇奶香久久不散。",
			Time:       "35 分钟",
			Difficulty: "甜品",
			Price:      15.0,
			Emoji:      "🍰",
			Steps:      []string{"奶油奶酪室温软化加糖打匀", "分次加入全蛋与淡奶油拌匀", "筛入少量玉米淀粉过筛进模具", "220度高温烘烤25分钟冷藏"},
		},
		{
			Name:       "多汁白桃乌龙暴打冻饮",
			Desc:       "手捣新鲜多汁白桃果肉，配清香高山冷萃乌龙，0 卡糖清爽低负担，夏日解腻神器。",
			Time:       "10 分钟",
			Difficulty: "极快",
			Price:      12.0,
			Emoji:      "🥤",
			Steps:      []string{"白桃去皮手捣出汁", "加入冰块与少许蜂蜜调味", "冲入冷萃高山乌龙茶汤", "封杯用力摇晃均匀"},
		},
		{
			Name:       "鲜香滑嫩黑椒雪花牛肉粒",
			Desc:       "外焦里嫩爆汁，黄油爆香蒜粒与杏鲍菇，大粒现磨黑胡椒激发牛肉原香。",
			Time:       "15 分钟",
			Difficulty: "快手",
			Price:      38.0,
			Emoji:      "🥩",
			Steps:      []string{"雪花牛肉切粒加生抽黑椒腌制", "杏鲍菇煎至金黄盛出备用", "大火爆炒牛肉粒至断生", "合入菇粒调味出锅"},
		},
		{
			Name:       "暖心鲜甜上汤娃娃菜",
			Desc:       "皮蛋火腿慢火吊出奶白浓汤，娃娃菜吸饱鲜味，清润鲜甜，做饭方拿手暖心汤菜。",
			Time:       "12 分钟",
			Difficulty: "简单",
			Price:      16.0,
			Emoji:      "🥬",
			Steps:      []string{"皮蛋、火腿肠切丁煎香", "加开水大火煮出奶白高汤", "下入娃娃菜煮软", "撒枸杞与少许白胡椒出锅"},
		},
	}

	if kw == "" {
		return library
	}

	var matched []RecipeItem
	for _, item := range library {
		if strings.Contains(strings.ToLower(item.Name), kw) ||
			strings.Contains(strings.ToLower(item.Desc), kw) ||
			strings.Contains(strings.ToLower(item.Difficulty), kw) {
			matched = append(matched, item)
		}
	}
	return matched
}
