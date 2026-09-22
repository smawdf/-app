package repository

import (
	"log"

	"github.com/glebarez/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"

	"orderdisk-server/internal/model"
)

func InitDB(dbPath string) (*gorm.DB, error) {
	db, err := gorm.Open(sqlite.Open(dbPath), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	if err != nil {
		return nil, err
	}

	// 自动同步与迁移表结构
	err = db.AutoMigrate(
		&model.User{},
		&model.Pair{},
		&model.Shop{},
		&model.Category{},
		&model.MenuItem{},
		&model.Order{},
		&model.OrderItem{},
		&model.CandyTransaction{},
	)
	if err != nil {
		return nil, err
	}

	log.Println("[DB] SQLite database initialized and auto-migrated successfully:", dbPath)
	return db, nil
}
