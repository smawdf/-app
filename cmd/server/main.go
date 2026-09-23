package main

import (
	"log"

	"github.com/gin-gonic/gin"

	"orderdisk-server/internal/config"
	"orderdisk-server/internal/handler"
	"orderdisk-server/internal/middleware"
	"orderdisk-server/internal/model"
	"orderdisk-server/internal/repository"
	"orderdisk-server/internal/service"
	"orderdisk-server/internal/ws"
)

func main() {
	cfg := config.LoadConfig()

	// 1. 初始化数据库与表结构迁移
	db, err := repository.InitDB(cfg.DBPath)
	if err != nil {
		log.Fatalf("Failed to initialize database: %v", err)
	}

	appService := service.NewAppService(db)
	h := handler.NewHandler(cfg, appService)

	r := gin.Default()

	// 跨域支持
	r.Use(func(c *gin.Context) {
		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
		c.Writer.Header().Set("Access-Control-Allow-Credentials", "true")
		c.Writer.Header().Set("Access-Control-Allow-Headers", "Content-Type, Content-Length, Accept-Encoding, X-CSRF-Token, Authorization, accept, origin, Cache-Control, X-Requested-With")
		c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, OPTIONS, GET, PUT, DELETE")
		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}
		c.Next()
	})

	// 2. WebSocket 实时通信端点 (免轮询长连接)
	r.GET("/ws", ws.HandleWS)

	api := r.Group("/api/v1")
	{
		// 公开认证接口
		auth := api.Group("/auth")
		{
			auth.POST("/register", h.Register)
			auth.POST("/login", h.Login)
		}

		// 需登录的业务接口
		authorized := api.Group("")
		authorized.Use(middleware.AuthRequired(cfg))
		{
			// 档案与配对
			authorized.GET("/me", h.Me)
			authorized.POST("/pairs", h.CreatePair)
			authorized.POST("/pairs/join", h.JoinPair)

			// 恋爱纪念日与时光记录
			authorized.GET("/anniversary", h.GetAnniversary)
			authorized.PUT("/anniversary", h.UpdateAnniversary)

			// 真实菜谱搜索
			authorized.GET("/recipes/search", h.SearchRecipes)

			// 小店与菜单（读写分离：查询双方可用，写操作仅饲养员）
			authorized.GET("/shop", h.GetShop)
			authorized.GET("/menu", h.ListMenu)

			// 订单查询
			authorized.GET("/orders", h.ListOrders)
			authorized.GET("/orders/:order_id", h.GetOrder)
			authorized.POST("/orders/:order_id/cancel", h.CancelOrder)

			// 糖币流水
			authorized.GET("/candy/transactions", h.ListCandyTransactions)

			// 饲养员专属能力
			caretaker := authorized.Group("")
			caretaker.Use(middleware.RequireRole(model.RoleCaretaker))
			{
				caretaker.PUT("/shop", h.UpdateShop)
				caretaker.POST("/candy/recharge", h.RechargeCandy)
				caretaker.PUT("/orders/:order_id/status", h.AdvanceOrderStatus)
				caretaker.POST("/menu", h.CreateMenuItem)
				caretaker.PUT("/menu/:item_id", h.UpdateMenuItem)
				caretaker.DELETE("/menu/:item_id", h.DeleteMenuItem)
			}

			// 吃货专属能力
			eater := authorized.Group("")
			eater.Use(middleware.RequireRole(model.RoleEater))
			{
				eater.POST("/orders", h.SubmitOrder)
			}
		}
	}

	log.Printf("[Server] OrderDisk backend server starting on port :%s ...", cfg.Port)
	if err := r.Run(":" + cfg.Port); err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
