package model

import (
	"time"

	"gorm.io/gorm"
)

// Role 常量定义
const (
	RoleCaretaker = "caretaker" // 饲养员（做饭方）
	RoleEater     = "eater"     // 吃货（点菜方）
)

// 订单流转状态常量
const (
	StatusSubmitted  = "submitted"  // 待接单
	StatusConfirmed  = "confirmed"  // 已接单备料
	StatusPreparing  = "preparing"  // 厨房烹饪中
	StatusDelivering = "delivering" // 端盘上桌
	StatusCompleted  = "completed"  // 开饭吃光
	StatusCancelled  = "cancelled"  // 已取消
)

// User 用户账号
type User struct {
	ID        string         `gorm:"primaryKey;size:36" json:"id"`
	Username  string         `gorm:"uniqueIndex;size:64;not null" json:"username"`
	Password  string         `gorm:"size:255;not null" json:"-"` // 不在 JSON 中输出哈希
	Nickname  string         `gorm:"size:64;not null" json:"nickname"`
	AvatarURL string         `gorm:"size:512" json:"avatar_url"`
	Role      string         `gorm:"size:20;default:'eater'" json:"role"` // 身份角色
	PairID    string         `gorm:"index;size:36" json:"pair_id"`       // 配对组 ID
	CreatedAt time.Time      `json:"created_at"`
	UpdatedAt time.Time      `json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}

// Pair 情侣双人配对关系
type Pair struct {
	ID            string         `gorm:"primaryKey;size:36" json:"id"`
	InviteCode    string         `gorm:"uniqueIndex;size:8;not null" json:"invite_code"` // 6位邀请码
	CaretakerID   string         `gorm:"size:36;index" json:"caretaker_id"`              // 饲养员用户 ID
	EaterID       string         `gorm:"size:36;index" json:"eater_id"`                  // 吃货用户 ID
	CandyCoins    int            `gorm:"default:66" json:"candy_coins"`                  // 共享糖糖币余额（初值66）
	AnniversaryAt *time.Time     `json:"anniversary_at"`                                 // 纪念日
	CreatedAt     time.Time      `json:"created_at"`
	UpdatedAt     time.Time      `json:"updated_at"`
	DeletedAt     gorm.DeletedAt `gorm:"index" json:"-"`
}

// Shop 单店模式（每对情侣专属的小店）
type Shop struct {
	ID           string         `gorm:"primaryKey;size:36" json:"id"`
	PairID       string         `gorm:"uniqueIndex;size:36;not null" json:"pair_id"` // 与情侣强绑定
	Name         string         `gorm:"size:64;not null" json:"name"`
	Announcement string         `gorm:"size:255" json:"announcement"`
	CoverURL     string         `gorm:"size:512" json:"cover_url"`
	CreatedAt    time.Time      `json:"created_at"`
	UpdatedAt    time.Time      `json:"updated_at"`
	DeletedAt    gorm.DeletedAt `gorm:"index" json:"-"`
}

// Category 菜单分类
type Category struct {
	ID        string         `gorm:"primaryKey;size:36" json:"id"`
	PairID    string         `gorm:"index;size:36;not null" json:"pair_id"`
	Name      string         `gorm:"size:32;not null" json:"name"`
	SortOrder int            `gorm:"default:0" json:"sort_order"`
	CreatedAt time.Time      `json:"created_at"`
	UpdatedAt time.Time      `json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`
}

// MenuItem 菜品
type MenuItem struct {
	ID          string         `gorm:"primaryKey;size:36" json:"id"`
	PairID      string         `gorm:"index;size:36;not null" json:"pair_id"`
	CategoryID  string         `gorm:"index;size:36" json:"category_id"`
	Name        string         `gorm:"size:64;not null" json:"name"`
	Description string         `gorm:"size:512" json:"description"`
	Price       float64        `gorm:"type:decimal(10,2);not null" json:"price"` // 原价/参考标价
	ImageURL    string         `gorm:"size:512" json:"image_url"`
	SalesCount  int            `gorm:"default:0" json:"sales_count"`
	IsAvailable bool           `gorm:"default:true" json:"is_available"`
	CreatedAt   time.Time      `json:"created_at"`
	UpdatedAt   time.Time      `json:"updated_at"`
	DeletedAt   gorm.DeletedAt `gorm:"index" json:"-"`
}

// Order 订单主表
type Order struct {
	ID              string         `gorm:"primaryKey;size:36" json:"id"`
	PairID          string         `gorm:"index;size:36;not null" json:"pair_id"`
	BuyerID         string         `gorm:"size:36;not null" json:"buyer_id"` // 必须由吃货下单
	BuyerName       string         `gorm:"size:64" json:"buyer_name"`
	BuyerAvatarURL  string         `gorm:"size:512" json:"buyer_avatar_url"`
	Status          string         `gorm:"size:20;default:'submitted';index" json:"status"`
	BuyerNote       string         `gorm:"size:255" json:"buyer_note"`
	Subtotal        float64        `gorm:"type:decimal(10,2);not null" json:"subtotal"`
	TotalPrice      float64        `gorm:"type:decimal(10,2);not null" json:"total_price"`
	CandyCoinsSpent int            `gorm:"not null" json:"candy_coins_spent"` // 本单消耗的糖糖币
	MomentImageURL  string         `gorm:"size:512" json:"moment_image_url"`  // 出锅实拍完成图（做饭记忆）
	Items           []OrderItem    `gorm:"foreignKey:OrderID" json:"items"`
	CreatedAt       time.Time      `json:"created_at"`
	UpdatedAt       time.Time      `json:"updated_at"`
	DeletedAt       gorm.DeletedAt `gorm:"index" json:"-"`
}

// OrderItem 订单菜品明细
type OrderItem struct {
	ID        string    `gorm:"primaryKey;size:36" json:"id"`
	OrderID   string    `gorm:"index;size:36;not null" json:"order_id"`
	MenuID    string    `gorm:"size:36" json:"menu_id"`
	Name      string    `gorm:"size:64;not null" json:"name"`
	ImageURL  string    `gorm:"size:512" json:"image_url"`
	UnitPrice float64   `gorm:"type:decimal(10,2);not null" json:"unit_price"`
	Quantity  int       `gorm:"not null" json:"quantity"`
	Subtotal  float64   `gorm:"type:decimal(10,2);not null" json:"subtotal"`
	CreatedAt time.Time `json:"created_at"`
}

// CandyTransaction 糖糖币收支流水账本
type CandyTransaction struct {
	ID          string    `gorm:"primaryKey;size:36" json:"id"`
	PairID      string    `gorm:"index;size:36;not null" json:"pair_id"`
	Amount      int       `gorm:"not null" json:"amount"`           // 正数为充值/退还，负数为下单消费
	Balance     int       `gorm:"not null" json:"balance"`          // 变动后余额
	ActionType  string    `gorm:"size:32;not null" json:"type"`     // recharge(撒糖), spend(点菜扣款), refund(退单退款)
	Description string    `gorm:"size:128" json:"description"`      // 备注说明
	ReferenceID string    `gorm:"size:36" json:"reference_id"`      // 关联的订单号或充值操作
	CreatedAt   time.Time `json:"created_at"`
}

// SweetMoment 做饭出锅时光回忆墙（对应原生 momentImageUrl 与时光纪念）
type SweetMoment struct {
	ID        string    `gorm:"primaryKey;size:36" json:"id"`
	PairID    string    `gorm:"index;size:36;not null" json:"pair_id"`
	OrderID   string    `gorm:"size:36" json:"order_id"`
	DishName  string    `gorm:"size:64;not null" json:"dish_name"`
	ImageURL  string    `gorm:"size:512" json:"image_url"`
	ChefName  string    `gorm:"size:64" json:"chef_name"`
	Note      string    `gorm:"size:512" json:"note"`
	Emoji     string    `gorm:"size:16" json:"emoji"`
	CookedAt  time.Time `json:"cooked_at"`
	CreatedAt time.Time `json:"created_at"`
}
