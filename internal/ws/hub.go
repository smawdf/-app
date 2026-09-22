package ws

import (
	"encoding/json"
	"log"
	"net/http"
	"sync"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true // 允许跨域
	},
}

// Event 实时通信消息体
type Event struct {
	Type    string      `json:"type"`    // order_created, order_updated, candy_changed, pair_joined, menu_updated
	Payload interface{} `json:"payload"` // 业务数据
}

// Hub 管理所有活跃的情侣 WebSocket 客户端
type Hub struct {
	// pairID -> []Client
	pairs map[string]map[*Client]bool
	mu    sync.RWMutex
}

var GlobalHub = &Hub{
	pairs: make(map[string]map[*Client]bool),
}

type Client struct {
	hub    *Hub
	conn   *websocket.Conn
	send   chan []byte
	pairID string
	userID string
}

func (h *Hub) Register(c *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if _, ok := h.pairs[c.pairID]; !ok {
		h.pairs[c.pairID] = make(map[*Client]bool)
	}
	h.pairs[c.pairID][c] = true
	log.Printf("[WS] Client connected: user=%s, pair=%s, total_in_pair=%d", c.userID, c.pairID, len(h.pairs[c.pairID]))
}

func (h *Hub) Unregister(c *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if clients, ok := h.pairs[c.pairID]; ok {
		if _, exists := clients[c]; exists {
			delete(clients, c)
			close(c.send)
			if len(clients) == 0 {
				delete(h.pairs, c.pairID)
			}
		}
	}
	log.Printf("[WS] Client disconnected: user=%s, pair=%s", c.userID, c.pairID)
}

// BroadcastToPair 广播给这对情侣的所有在线设备（一人动作，伴侣秒收到）
func (h *Hub) BroadcastToPair(pairID string, eventType string, payload interface{}) {
	h.mu.RLock()
	clients, ok := h.pairs[pairID]
	h.mu.RUnlock()

	if !ok || len(clients) == 0 {
		return
	}

	msg, err := json.Marshal(Event{Type: eventType, Payload: payload})
	if err != nil {
		log.Printf("[WS] Marshal event error: %v", err)
		return
	}

	for client := range clients {
		select {
		case client.send <- msg:
		default:
			close(client.send)
			delete(clients, client)
		}
	}
}

// HandleWS 连接入口
func HandleWS(c *gin.Context) {
	pairID := c.Query("pair_id")
	userID := c.Query("user_id")
	if pairID == "" || userID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "pair_id and user_id required"})
		return
	}

	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Printf("[WS] Upgrade error: %v", err)
		return
	}

	client := &Client{
		hub:    GlobalHub,
		conn:   conn,
		send:   make(chan []byte, 256),
		pairID: pairID,
		userID: userID,
	}

	client.hub.Register(client)

	go client.writePump()
	go client.readPump()
}

func (c *Client) readPump() {
	defer func() {
		c.hub.Unregister(c)
		c.conn.Close()
	}()
	for {
		_, _, err := c.conn.ReadMessage()
		if err != nil {
			break
		}
	}
}

func (c *Client) writePump() {
	defer c.conn.Close()
	for message := range c.send {
		err := c.conn.WriteMessage(websocket.TextMessage, message)
		if err != nil {
			break
		}
	}
}
