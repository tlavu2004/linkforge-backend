CREATE TABLE short_links (
    id BIGINT PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE,
    click_count BIGINT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    -- delete_token_hash holds SHA-256 hash
    delete_token_hash VARCHAR(64),
    
    CONSTRAINT uk_short_links_short_code UNIQUE (short_code)
);

CREATE INDEX idx_short_links_created_at ON short_links (created_at);
-- Partial index for expiration cleanup job
CREATE INDEX idx_short_links_expires_at ON short_links (expires_at) WHERE is_active = true;
