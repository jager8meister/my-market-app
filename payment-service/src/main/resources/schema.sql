-- Таблица для хранения платежей
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Индекс для быстрого поиска платежей по заказу
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);

-- Индекс для поиска по пользователю
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);

-- Индекс для поиска по статусу
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

-- Таблица для хранения балансов пользователей
CREATE TABLE IF NOT EXISTS user_balances (
    user_id BIGINT PRIMARY KEY,
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
