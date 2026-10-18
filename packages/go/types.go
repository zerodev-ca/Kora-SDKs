package kora

import "time"

type ValidationResponse struct {
	Valid     bool           `json:"valid"`
	Code      string         `json:"code"`
	Message   string         `json:"message"`
	License   map[string]any `json:"license"`
	Timestamp int64          `json:"timestamp"`
}

type DeactivationResponse struct {
	Success   bool   `json:"success"`
	Code      string `json:"code"`
	Message   string `json:"message"`
	Removed   int    `json:"removed"`
	Timestamp int64  `json:"timestamp"`
}

type OfflineLicense struct {
	Key          string              `json:"key"`
	LicenseID    int64               `json:"license_id"`
	Status       string              `json:"status"`
	Product      map[string]any      `json:"product"`
	Customer     map[string]any      `json:"customer"`
	Addons       []string            `json:"addons"`
	Whitelists   map[string][]string `json:"whitelists"`
	ExpiresAt    *time.Time          `json:"expires_at"`
	IssuedAt     time.Time           `json:"issued_at"`
	RefreshAfter time.Time           `json:"refresh_after"`
}

type Page struct {
	Total int
	Rows  []map[string]any
}
