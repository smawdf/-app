package handler

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"orderdisk-server/internal/service"
)

// UpdateShop 饲养员修改小店名称与公告
func (h *Handler) UpdateShop(c *gin.Context) {
	pairID := c.GetString("pair_id")
	var in service.ShopInput
	if err := c.ShouldBindJSON(&in); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "参数格式错误"})
		return
	}

	shop, err := h.svc.UpdateShop(pairID, in)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, shop)
}

// GetAnniversary 查看纪念日与出锅回忆墙
func (h *Handler) GetAnniversary(c *gin.Context) {
	pairID := c.GetString("pair_id")
	info, err := h.svc.GetAnniversary(pairID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, info)
}

// UpdateAnniversary 设置恋爱纪念日起始日
func (h *Handler) UpdateAnniversary(c *gin.Context) {
	pairID := c.GetString("pair_id")
	var in service.AnniversaryInput
	if err := c.ShouldBindJSON(&in); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "参数格式错误"})
		return
	}

	info, err := h.svc.UpdateAnniversary(pairID, in)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, info)
}

// SearchRecipes 真实菜谱检索
func (h *Handler) SearchRecipes(c *gin.Context) {
	kw := c.Query("keyword")
	recipes := h.svc.SearchRecipes(kw)
	c.JSON(http.StatusOK, gin.H{"recipes": recipes})
}
