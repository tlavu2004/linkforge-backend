CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    is_vip BOOLEAN NOT NULL DEFAULT false,
    vip_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users (email);

ALTER TABLE short_links
ADD CONSTRAINT fk_short_links_user 
    FOREIGN KEY (user_id) 
    REFERENCES users(id) 
    ON DELETE SET NULL;
