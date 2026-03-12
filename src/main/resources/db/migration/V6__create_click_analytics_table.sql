-- Migration: V6__create_click_analytics_table
-- Description: Create click_analytics table for detailed tracking

CREATE TABLE click_analytics (
    id BIGINT PRIMARY KEY,
    short_code VARCHAR(15) NOT NULL,
    clicked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ip_address VARCHAR(45),
    user_agent TEXT,
    country VARCHAR(3),
    city VARCHAR(100),
    device_type VARCHAR(10),
    referrer TEXT
);

-- Index for efficient analytics queries
CREATE INDEX idx_click_analytics_short_code_time ON click_analytics (short_code, clicked_at);
