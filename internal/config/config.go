package config

import "os"

type Config struct {
	Port      string
	DBPath    string
	JWTSecret string
}

func LoadConfig() *Config {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	dbPath := os.Getenv("DB_PATH")
	if dbPath == "" {
		dbPath = "./orderdisk.db"
	}
	secret := os.Getenv("JWT_SECRET")
	if secret == "" {
		secret = "orderdisk_cozy_couple_secret_key_2026"
	}
	return &Config{
		Port:      port,
		DBPath:    dbPath,
		JWTSecret: secret,
	}
}
