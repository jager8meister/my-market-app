-- Тестовые пользователи с BCrypt-хешированными паролями
-- BCrypt для 'password1': $2a$12$ZmDZuRJUNJZwaGe0q/1rPe81CIbIp1GTtqjELc6vNMaNt/L48/jPm
-- BCrypt для 'password2': $2a$12$on5jE53VVhc0PpdpTRrvqumFfOEa4Y6PTxB7zgyNmQwF.jOM0BblK
-- BCrypt для 'password3': $2a$12$Eb058J.fHdfqAII41Oz6PuHw0Guft56qJcelnbdPtjtzeZu9u0hIi
INSERT INTO users (username, password, balance, created_at) VALUES
('user1', '$2a$12$ZmDZuRJUNJZwaGe0q/1rPe81CIbIp1GTtqjELc6vNMaNt/L48/jPm', 200000, CURRENT_TIMESTAMP),
('user2', '$2a$12$on5jE53VVhc0PpdpTRrvqumFfOEa4Y6PTxB7zgyNmQwF.jOM0BblK', 50000, CURRENT_TIMESTAMP),
('user3', '$2a$12$Eb058J.fHdfqAII41Oz6PuHw0Guft56qJcelnbdPtjtzeZu9u0hIi', 1000, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

INSERT INTO items (title, description, price, img_path) VALUES
('Смартфон A1', 'Базовый смартфон с экраном 6.1"', 17990, 'images/android_phone.png'),
('Смартфон A2', 'Смартфон с двойной камерой и NFC', 24990, 'images/android_phone.png'),
('Смартфон A3', 'Продвинутый смартфон с большим аккумулятором', 34990, 'images/android_phone.png'),
('Смартфон Pro', 'Флагманский смартфон с OLED-дисплеем', 64990, 'images/iphone.png'),
('Смартфон Max', 'Смартфон с большим экраном 6.8"', 89990, 'images/iphone.png'),

('Ноутбук Office 13"', 'Лёгкий ноутбук для работы и учебы', 49990, 'images/laptop_light.png'),
('Ноутбук Office 15"', 'Ноутбук с 15-дюймовым экраном и SSD', 64990, 'images/laptop_white.png'),
('Ноутбук Gaming 15"', 'Игровой ноутбук с дискретной видеокартой', 119990, 'images/laptop_dark.png'),
('Ноутбук Ultrabook', 'Тонкий ультрабук с алюминиевым корпусом', 89990, 'images/laptop_on_the_table.png'),
('Ноутбук Workstation', 'Мощная мобильная рабочая станция', 179990, 'images/laptop_black.png'),

('Наушники Lite', 'Лёгкие проводные наушники для повседневного использования', 1990, 'images/headphones.png'),
('Наушники Studio', 'Полноразмерные наушники с чистым звуком', 8990, 'images/headphones.png'),
('Наушники Wireless', 'Беспроводные наушники с Bluetooth 5.3', 12990, 'images/headphones.png'),
('Наушники Noise Cancel', 'Наушники с активным шумоподавлением', 24990, 'images/headphones.png'),
('Наушники Sport', 'Защитные от пота наушники для тренировок', 6490, 'images/headphones.png'),

('Планшет Mini', 'Компактный планшет с 8-дюймовым экраном', 17990, 'images/tablet.png'),
('Планшет 10"', 'Планшет для мультимедиа и интернета', 29990, 'images/tablet.png'),
('Монитор 24"', 'IPS-монитор 24\" для дома и офиса', 14990, 'images/laptop_light.png'),
('Монитор 27"', 'Монитор 27\" с высокой частотой обновления', 27990, 'images/laptop_dark.png'),
('Клавиатура', 'Мембранная клавиатура с тихими клавишами', 1490, 'images/laptop_white.png')
ON CONFLICT DO NOTHING;

