-- Таблица товаров
DROP TABLE IF EXISTS item_images CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS items CASCADE;

CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price BIGINT NOT NULL,
    img_path VARCHAR(255)
);

-- Таблица изображений товаров
CREATE TABLE item_images (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    data BYTEA NOT NULL,
    content_type VARCHAR(100),
    CONSTRAINT fk_item_images_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    CONSTRAINT uk_item_images_item_id UNIQUE (item_id)
);

-- Таблица заказов
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    total_sum BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Таблица позиций заказа
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    price BIGINT NOT NULL,
    count INTEGER NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Индексы для оптимизации поиска
CREATE INDEX IF NOT EXISTS idx_items_title ON items(title);
CREATE INDEX IF NOT EXISTS idx_items_price ON items(price);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
