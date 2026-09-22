package handler

import (
	"net/http"

	"github.com/gin-gonic/gin"

	"orderdisk-server/internal/middleware"
	"orderdisk-server/internal/service"
)

// pairScope 从鉴权上下文取出 pairID，未配对时直接返回 400
func (h *Handler) pairScope(c *gin.Context) (userID, role, pairID string, ok bool) {
	userID, role, pairID, err := middleware.GetContextUser(c)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
		return "", "", "", false
	}
	if pairID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "NOT_PAIRED", "message": "请先和伴侣完成配对绑定"})
		return "", "", "", false
	}
	return userID, role, pairID, true
}

// Me 返回当前用户档案 + 配对关系 + 小店信息
func (h *Handler) Me(c *gin.Context) {
	userID, _, _, ok := h.pairScope(c)
	if !ok {
		// 未配对也要能拿到自己的用户信息，供前端进入配对页
		uid, _, _, err := middleware.GetContextUser(c)
		if err != nil {
			c.JSON(http.StatusUnauthorized, gin.H{"error": err.Error()})
			return
		}
		user, pair, shop, err := h.svc.Me(uid)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusOK, gin.H{"user": user, "pair": pair, "shop": shop, "paired": false})
		return
	}

	user, pair, shop, err := h.svc.Me(userID)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"user": user, "pair": pair, "shop": shop, "paired": true})
}

// GetShop 小店信息
func (h *Handler) GetShop(c *gin.Context) {
	_, _, pairID, ok := h.pairScope(c)
	if !ok {
		return
	}
	shop, err := h.svc.GetShop(pairID)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, shop)
}

// ListMenu 菜单列表（吃货点菜 / 饲养员管理共用）
func (h *Handler) ListMenu(c *gin.Context) {
	_, _, pairID, ok := h.pairScope(c)
	if !ok {
		return
	}
	items, err := h.svc.ListMenu(pairID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"items": items})
}

// CreateMenuItem 饲养员上新菜品
func (h *Handler) CreateMenuItem(c *gin.Context) {
	_, _, pairID, ok := h.pairScope(c)
	if !ok {
		return
	}
	var in service.MenuItemInput
	if err := c.ShouldBindJSON(&in); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	item, err := h.svc.CreateMenuItem(pairID, in)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, item)
}

// UpdateMenuItem 饲养员编辑菜品
func (h *Handler) UpdateMenuItem(c *gin.Context) {
	_, _, pairID, ok := h.pairScope(c)
	if !ok {
		return
	}
	var in service.MenuItemInput
	if err := c.ShouldBindJSON(&in); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	item, err := h.svc.UpdateMenuItem(pairID, c.Param("item_id"), in)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, item)
}

// DeleteMenuItem 饲养员下架菜品
func (h *Handler) DeleteMenuItem(c *gin.Context) {
	_, _, pairID, ok := h.pairScope(c)
	if !ok {
		return
	}
	if err := h.svc.DeleteMenuItem(pairID, c.Param("item_id")); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "菜品已下架"})
}

// ListOrders 订单列表
func (h *Handler) ListOrders(c *gin.Context) {
	_, _, pairID, ok := h.pairScope(c)
	if !ok {
		return
	}
	orders, err := h.svc.ListOrders(pairID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"orders": orders})
}

// GetOrder 订单详情
func (h *Handler) GetOrder(c *gin.Context) {
	_, _, pairID, ok := h.pairScope(c)
	if !ok {
		return
	}
	order, err := h.svc.GetOrder(pairID, c.Param("order_id"))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, order)
}

// ListCandyTransactions 糖糖币流水账本
func (h *Handler) ListCandyTransactions(c *gin.Context) {
	_, _, pairID, ok := h.pairScope(c)
	if !ok {
		return
	}
	list, err := h.svc.ListCandyTransactions(pairID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"transactions": list})
}
