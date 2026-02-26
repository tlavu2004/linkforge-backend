ALTER TABLE short_links
ADD COLUMN user_id BIGINT;

ALTER TABLE short_links
ADD CONSTRAINT fk_short_links_user 
    FOREIGN KEY (user_id) 
    REFERENCES users(id) 
    ON DELETE SET NULL;

CREATE INDEX idx_short_links_user_id ON short_links(user_id);
