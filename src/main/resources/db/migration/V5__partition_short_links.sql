-- Migration: V5__partition_short_links
-- Description: Convert short_links to a range-partitioned table by created_at

-- 1. Create the partitioned table with the same structure
CREATE TABLE short_links_partitioned (
    id BIGINT NOT NULL,
    short_code VARCHAR(10) NOT NULL,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE,
    click_count BIGINT NOT NULL DEFAULT 0,
    delete_token_hash VARCHAR(64),
    user_id BIGINT,
    qr_code TEXT,
    
    -- Composite primary key including the partition key
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 2. Create monthly partitions for the current and next 3 months
-- Today is 2026-03, so we create for March, April, May, June
CREATE TABLE short_links_y2026_m03 PARTITION OF short_links_partitioned
    FOR VALUES FROM ('2026-03-01 00:00:00+00') TO ('2026-04-01 00:00:00+00');

CREATE TABLE short_links_y2026_m04 PARTITION OF short_links_partitioned
    FOR VALUES FROM ('2026-04-01 00:00:00+00') TO ('2026-05-01 00:00:00+00');

CREATE TABLE short_links_y2026_m05 PARTITION OF short_links_partitioned
    FOR VALUES FROM ('2026-05-01 00:00:00+00') TO ('2026-06-01 00:00:00+00');

CREATE TABLE short_links_y2026_m06 PARTITION OF short_links_partitioned
    FOR VALUES FROM ('2026-06-01 00:00:00+00') TO ('2026-07-01 00:00:00+00');

-- 3. Create a default partition for data outside ranges (safety)
CREATE TABLE short_links_default PARTITION OF short_links_partitioned DEFAULT;

-- 4. Move data from the old table to the new one
INSERT INTO short_links_partitioned (id, short_code, original_url, created_at, expires_at, click_count, delete_token_hash, user_id, qr_code)
SELECT id, short_code, original_url, created_at, expires_at, click_count, delete_token_hash, user_id, qr_code FROM short_links;

-- 5. Swap tables
DROP TABLE short_links CASCADE;
ALTER TABLE short_links_partitioned RENAME TO short_links;

-- 6. Re-create indexes
-- Note: UNIQUE constraints must include the partition key in PostgreSQL
CREATE UNIQUE INDEX uk_short_links_short_code ON short_links (short_code, created_at);
CREATE INDEX idx_short_links_expires_at ON short_links (expires_at);
CREATE INDEX idx_short_links_user_id ON short_links (user_id);
-- No need for idx_short_links_created_at as it's the partition key
