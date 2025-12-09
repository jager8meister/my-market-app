INSERT INTO items (title, description, price, img_path) VALUES
('Смартфон A1', 'Базовый смартфон с экраном 6.1"', 19990, 'images/android_phone.png'),
('Смартфон A2', 'Смартфон с двойной камерой и NFC', 24990, 'images/android_phone.png'),
('Смартфон A3', 'Продвинутый смартфон с большим аккумулятором', 29990, 'images/android_phone.png'),
('Смартфон Pro', 'Флагманский смартфон с OLED-дисплеем', 39990, 'images/iphone.png'),
('Смартфон Max', 'Смартфон с большим экраном 6.8"', 44990, 'images/iphone.png'),

('Ноутбук Office 13"', 'Лёгкий ноутбук для работы и учебы', 59990, 'images/laptop_light.png'),
('Ноутбук Office 15"', 'Ноутбук с 15-дюймовым экраном и SSD', 64990, 'images/laptop_white.png'),
('Ноутбук Gaming 15"', 'Игровой ноутбук с дискретной видеокартой', 89990, 'images/laptop_dark.png'),
('Ноутбук Ultrabook', 'Тонкий ультрабук с алюминиевым корпусом', 99990, 'images/laptop_on_the_table.png'),
('Ноутбук Workstation', 'Мощная мобильная рабочая станция', 129990, 'images/laptop_black.png'),

('Наушники Lite', 'Лёгкие проводные наушники для повседневного использования', 2990, 'images/headphones.png'),
('Наушники Studio', 'Полноразмерные наушники с чистым звуком', 7990, 'images/headphones.png'),
('Наушники Wireless', 'Беспроводные наушники с Bluetooth 5.3', 9990, 'images/headphones.png'),
('Наушники Noise Cancel', 'Наушники с активным шумоподавлением', 14990, 'images/headphones.png'),
('Наушники Sport', 'Защитные от пота наушники для тренировок', 5990, 'images/headphones.png'),

('Планшет Mini', 'Компактный планшет с 8-дюймовым экраном', 19990, 'images/tablet.png'),
('Планшет 10"', 'Планшет для мультимедиа и интернета', 24990, 'images/tablet.png'),
('Монитор 24"', 'IPS-монитор 24\" для дома и офиса', 15990, 'images/laptop_light.png'),
('Монитор 27"', 'Монитор 27\" с высокой частотой обновления', 22990, 'images/laptop_dark.png'),
('Клавиатура', 'Мембранная клавиатура с тихими клавишами', 1990, 'images/laptop_white.png')
ON CONFLICT DO NOTHING;

