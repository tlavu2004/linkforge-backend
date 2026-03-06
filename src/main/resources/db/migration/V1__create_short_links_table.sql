CREATE TABLE short_links (
    id BIGINT PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE,
    click_count BIGINT NOT NULL DEFAULT 0,
    delete_token_hash VARCHAR(64),
    user_id BIGINT,
    qr_code TEXT,
    
    CONSTRAINT uk_short_links_short_code UNIQUE (short_code)
);

CREATE INDEX idx_short_links_created_at ON short_links (created_at);
CREATE INDEX idx_short_links_expires_at ON short_links (expires_at);
CREATE INDEX idx_short_links_user_id ON short_links(user_id);
