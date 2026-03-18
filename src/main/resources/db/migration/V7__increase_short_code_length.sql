-- Migration: V7__increase_short_code_length
-- Description: Increase short_code length to accommodate custom aliases

ALTER TABLE short_links ALTER COLUMN short_code TYPE VARCHAR(50);
