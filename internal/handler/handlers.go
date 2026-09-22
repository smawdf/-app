package handler

import (
	"net/http"

	"github.com/gin-gonic/gin"

	"orderdisk-server/internal/config"
	"orderdisk-server/internal/middleware"
	"orderdisk-server/internal/service"
)

type Handler struct {
	cfg *config.Config
	svc *service.AppService
}

func NewHandler(cfg *config.Config, svc *service.AppService) *Handler {
	return &Handler{cfg: cfg, svc: svc}
}

// -------------------------------------------------------------
// 认证与注册
// -------------------------------------------------------------

type RegisterReq struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
	Nickname string `json:"nickname" binding:"required"`
	Role     string `json:"role" binding:"required,oneof=caretaker eater"`
}

func (h *Handler) Register(c *gin.Context) {
	var req RegisterReq
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	user, err := h.svc.Register(req.Username, req.Password, req.Nickname, req.Role)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	token, _ := middleware.GenerateToken(h.cfg, user)
	c.JSON(http.StatusOK, gin.H{
		"token": token,
		"user":  user,
	})
}

type LoginReq struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
}

func (h *Handler) Login(c *gin.Context) {
	var req LoginReq
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	user, err := h.svc.Login(req.Username, req.Password)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
		return
	}

	token, _ := middleware.GenerateToken(h.cfg, user)
	c.JSON(http.StatusOK, gin.H{
		"token": token,
		"user":  user,
	})
}

// -------------------------------------------------------------
// 配对接口
// -------------------------------------------------------------

func (h *Handler) CreatePair(c *gin.Context) {
	userID, role, _, err := middleware.GetContextUser(c)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
		return
	}

	pair, err := h.svc.CreatePair(userID, role)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// 配对后重新签发 Token，使 Token 内的 pair_id 立即生效
	token := ""
	if user, _, _, err := h.svc.Me(userID); err == nil && user != nil {
		token, _ = middleware.GenerateToken(h.cfg, user)
	}

	c.JSON(http.StatusOK, gin.H{
		"id":           pair.ID,
		"invite_code":  pair.InviteCode,
		"caretaker_id": pair.CaretakerID,
		"eater_id":     pair.EaterID,
		"candy_coins":  pair.CandyCoins,
		"token":        token,
	})
}

type JoinPairReq struct {
	InviteCode string `json:"invite_code" binding:"required"`
}

func (h *Handler) JoinPair(c *gin.Context) {
	userID, role, _, err := middleware.GetContextUser(c)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
		return
	}

	var req JoinPairReq
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	pair, err := h.svc.JoinPair(userID, role, req.InviteCode)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// 绑定伴侣后重新签发 Token，立即携带 pair_id
	token := ""
	if user, _, _, err := h.svc.Me(userID); err == nil && user != nil {
		token, _ = middleware.GenerateToken(h.cfg, user)
	}

	c.JSON(http.StatusOK, gin.H{
		"id":           pair.ID,
		"invite_code":  pair.InviteCode,
		"caretaker_id": pair.CaretakerID,
		"eater_id":     pair.EaterID,
		"candy_coins":  pair.CandyCoins,
		"token":        token,
	})
}

// -------------------------------------------------------------
// 糖糖币接口
// -------------------------------------------------------------

type RechargeReq struct {
	Amount int    `json:"amount" binding:"required,gt=0"`
	Reason string `json:"reason"`
}

func (h *Handler) RechargeCandy(c *gin.Context) {
	userID, _, pairID, err := middleware.GetContextUser(c)
	if err != nil || pairID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "未绑定伴侣或缺失鉴权"})
		return
	}

	var req RechargeReq
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	pair, err := h.svc.RechargeCandyCoins(pairID, userID, req.Amount, req.Reason)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"candy_coins": pair.CandyCoins,
		"message":     "撒糖成功，对方已实时收到提醒！🍬",
	})
}

// -------------------------------------------------------------
// 订单操作接口
// -------------------------------------------------------------

func (h *Handler) SubmitOrder(c *gin.Context) {
	userID, _, pairID, err := middleware.GetContextUser(c)
	if err != nil || pairID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请先绑定伴侣后再下单"})
		return
	}

	var req service.CreateOrderDTO
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	order, err := h.svc.SubmitOrder(userID, pairID, req)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, order)
}

type AdvanceStatusReq struct {
	NewStatus string `json:"new_status" binding:"required"`
}

func (h *Handler) AdvanceOrderStatus(c *gin.Context) {
	userID, _, pairID, err := middleware.GetContextUser(c)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
		return
	}

	orderID := c.Param("order_id")
	var req AdvanceStatusReq
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	order, err := h.svc.AdvanceOrderStatus(userID, pairID, orderID, req.NewStatus)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, order)
}

func (h *Handler) CancelOrder(c *gin.Context) {
	userID, _, pairID, err := middleware.GetContextUser(c)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
		return
	}

	orderID := c.Param("order_id")
	if err := h.svc.CancelOrder(userID, pairID, orderID); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "订单已取消，糖币已全额退还"})
}
