-- Migration: V4__create_payment_transactions_table
-- Description: Create payment_transactions table

CREATE TABLE payment_transactions (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_code VARCHAR(50) NOT NULL UNIQUE,
    package_code VARCHAR(50),
    amount INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_payment_transactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_transactions_order_code ON payment_transactions(order_code);
CREATE INDEX idx_payment_transactions_user_id ON payment_transactions(user_id);
