package middleware

import (
	"errors"
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"

	"orderdisk-server/internal/config"
	"orderdisk-server/internal/model"
)

type Claims struct {
	UserID string `json:"user_id"`
	Role   string `json:"role"`
	PairID string `json:"pair_id"`
	jwt.RegisteredClaims
}

// GenerateToken 生成 JWT Token
func GenerateToken(cfg *config.Config, user *model.User) (string, error) {
	claims := Claims{
		UserID: user.ID,
		Role:   user.Role,
		PairID: user.PairID,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(30 * 24 * time.Hour)), // 30 天免登
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			Issuer:    "orderdisk-server",
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(cfg.JWTSecret))
}

// AuthRequired JWT 鉴权拦截器
func AuthRequired(cfg *config.Config) gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Authorization header required"})
			return
		}

		parts := strings.SplitN(authHeader, " ", 2)
		if !(len(parts) == 2 && parts[0] == "Bearer") {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Invalid authorization format"})
			return
		}

		tokenString := parts[1]
		claims := &Claims{}
		token, err := jwt.ParseWithClaims(tokenString, claims, func(token *jwt.Token) (interface{}, error) {
			return []byte(cfg.JWTSecret), nil
		})

		if err != nil || !token.Valid {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Invalid or expired token"})
			return
		}

		// 存入上下文
		c.Set("user_id", claims.UserID)
		c.Set("role", claims.Role)
		c.Set("pair_id", claims.PairID)
		c.Next()
	}
}

// RequireRole 双角色权限硬隔离中间件
func RequireRole(expectedRole string) gin.HandlerFunc {
	return func(c *gin.Context) {
		roleVal, exists := c.Get("role")
		if !exists {
			c.AbortWithStatusJSON(http.StatusForbidden, gin.H{"error": "Role context missing"})
			return
		}
		role, _ := roleVal.(string)
		if role != expectedRole {
			if expectedRole == model.RoleEater {
				c.AbortWithStatusJSON(http.StatusForbidden, gin.H{
					"error": "EATER_ROLE_REQUIRED",
					"message": "只有【吃货】才拥有加购选菜与提交订单的特权哦！",
				})
			} else {
				c.AbortWithStatusJSON(http.StatusForbidden, gin.H{
					"error": "CARETAKER_ROLE_REQUIRED",
					"message": "只有【饲养员】才可以推进做饭状态、管理菜单和小店哦！",
				})
			}
			return
		}
		c.Next()
	}
}

func GetContextUser(c *gin.Context) (userID, role, pairID string, err error) {
	u, uOk := c.Get("user_id")
	r, rOk := c.Get("role")
	p, pOk := c.Get("pair_id")
	if !uOk || !rOk || !pOk {
		return "", "", "", errors.New("auth context missing")
	}
	return u.(string), r.(string), p.(string), nil
}
