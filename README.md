# My Market - Реактивный интернет-магазин

Мультимодульное приложение интернет-магазина на реактивном стеке Spring WebFlux с микросервисной архитектурой, кешированием в Redis и интеграцией с платежным сервисом.

## Архитектура проекта

Проект состоит из трех модулей:

- **api-contracts** - OpenAPI спецификации для интеграции между сервисами
- **market-app** - основное веб-приложение интернет-магазина
- **payment-service** - микросервис обработки платежей

## Технологический стек

### Backend
- Java 21
- Spring Boot 3.3.4
- Spring WebFlux (реактивный веб-фреймворк)
- Spring Data R2DBC (реактивная работа с БД)
- Spring Data Redis Reactive (реактивное кеширование)
- PostgreSQL 16 (основная БД)
- Redis 7 (кеширование)

### Инструменты
- MapStruct (маппинг DTO)
- Lombok (сокращение boilerplate кода)
- OpenAPI Generator (генерация клиентского/серверного кода)
- Thymeleaf (шаблонизатор)
- Docker & Docker Compose
- Testcontainers (интеграционное тестирование)

## Функциональность

### Market App (основное приложение)
- Каталог товаров с пагинацией, поиском и сортировкой
- Корзина покупок (хранится в памяти/сессии)
- Оформление заказов с интеграцией платежного сервиса
- История заказов
- Кеширование товаров и изображений в Redis
- Реактивная обработка запросов

### Payment Service (сервис платежей)
- Создание платежей
- Проверка статуса платежа
- Отмена платежа
- RESTful API на Spring WebFlux
- Собственная база данных PostgreSQL

## Как запустить

### Вариант 1: Docker Compose (рекомендуется)

Запустить все сервисы (БД, Redis, приложения):

```bash
mvn clean package -DskipTests
docker-compose up --build
```

Приложения будут доступны:
- Market App: http://localhost:8080
- Payment Service: http://localhost:8081
- Swagger UI Market App: http://localhost:8080/swagger-ui.html
- Swagger UI Payment Service: http://localhost:8081/swagger-ui.html

### Вариант 2: Локальный запуск

Требования: Java 21, Maven 3.8+, PostgreSQL 16, Redis 7

1. **Создать базы данных**:

```sql
-- Для market-app
CREATE DATABASE my_market_db;
CREATE USER my_market_user WITH PASSWORD 'my_market_password';
GRANT ALL PRIVILEGES ON DATABASE my_market_db TO my_market_user;

-- Для payment-service
CREATE DATABASE payment_db;
CREATE USER payment_user WITH PASSWORD 'payment_password';
GRANT ALL PRIVILEGES ON DATABASE payment_db TO payment_user;
```

2. **Запустить Redis**:

```bash
docker run -d -p 6379:6379 redis:7-alpine
```

3. **Собрать проект**:

```bash
mvn clean install
```

4. **Запустить payment-service**:

```bash
cd payment-service
mvn spring-boot:run
```

5. **Запустить market-app** (в отдельном терминале):

```bash
cd market-app
mvn spring-boot:run
```

## Тестирование

### Запуск unit-тестов

```bash
mvn test
```

### Запуск интеграционных тестов

```bash
mvn test -P integration-test
```

Интеграционные тесты используют Testcontainers для автоматического поднятия PostgreSQL и Redis.

### Покрытие кода

Отчет JaCoCo генерируется при запуске тестов:

```bash
mvn clean test
# Отчет: target/site/jacoco/index.html
```

## API Endpoints

### Market App

**Веб-страницы:**
- `GET /` - главная страница, каталог товаров
- `GET /items/{id}` - карточка товара
- `GET /cart/items` - корзина
- `GET /orders` - история заказов
- `GET /orders/{id}` - детали заказа

**REST API:**
- `GET /api/items` - список товаров (пагинация, фильтрация, сортировка)
- `GET /api/items/{id}` - детали товара
- `GET /api/items/{id}/image` - изображение товара
- `GET /api/cart` - состояние корзины
- `POST /api/cart` - обновить корзину
- `DELETE /api/cart` - очистить корзину
- `POST /api/orders` - создать заказ
- `GET /api/orders` - список заказов
- `GET /api/orders/{id}` - детали заказа

**Параметры запросов:**
- `?pageNumber=0&pageSize=10` - пагинация
- `?search=смартфон` - поиск по названию и описанию
- `?sort=ALPHA|PRICE|NO` - сортировка

### Payment Service

- `POST /api/payments` - создать платеж
- `GET /api/payments/{id}` - получить статус платежа
- `POST /api/payments/{id}/cancel` - отменить платеж

## Кеширование в Redis

Реализовано кеширование для оптимизации производительности:

### Кешируемые данные

| Данные | Ключ кеша | TTL |
|--------|-----------|-----|
| Детали товара | `item:{id}` | 1 час |
| Изображения товаров | `item-image:{id}` | 24 часа |

### Особенности кеширования

- Реактивное кеширование через `ReactiveRedisTemplate`
- Cache-aside pattern с автоматическим fallback на БД при ошибках Redis
- Подробное логирование (Cache HIT/MISS)
- Настраиваемый TTL для разных типов данных
- Поддержка инвалидации кеша по ключу и паттерну

## Структура проекта

```
my-market-app/
├── api-contracts/          # OpenAPI спецификации
│   └── src/main/resources/
│       └── payment-api.yaml
├── market-app/             # Основное приложение
│   ├── src/main/java/
│   │   └── ru/yandex/practicum/mymarket/
│   │       ├── config/          # Конфигурация (Redis, WebClient)
│   │       ├── controllers/     # REST контроллеры
│   │       ├── dto/            # DTO запросов/ответов
│   │       ├── entity/         # JPA сущности
│   │       ├── exception/      # Обработка ошибок
│   │       ├── mapper/         # MapStruct маппинг
│   │       ├── repository/     # Spring Data репозитории
│   │       └── service/        # Бизнес-логика, кеширование
│   └── src/test/            # Unit и интеграционные тесты
├── payment-service/         # Сервис платежей
│   ├── src/main/java/
│   │   └── ru/yandex/practicum/payment/
│   │       ├── controller/     # REST контроллеры
│   │       ├── entity/        # Сущности платежей
│   │       ├── mapper/        # MapStruct маппинг
│   │       ├── repository/    # Репозитории
│   │       └── service/       # Логика обработки платежей
│   └── src/test/            # Тесты
├── docker-compose.yml       # Docker Compose конфигурация
└── pom.xml                 # Parent POM
```

## Конфигурация

### Переменные окружения Market App

| Переменная | Описание | Значение по умолчанию |
|------------|----------|----------------------|
| `SPRING_R2DBC_URL` | URL базы данных | `r2dbc:postgresql://localhost:5432/my_market_db` |
| `SPRING_R2DBC_USERNAME` | Пользователь БД | `my_market_user` |
| `SPRING_R2DBC_PASSWORD` | Пароль БД | `my_market_password` |
| `REDIS_HOST` | Хост Redis | `localhost` |
| `REDIS_PORT` | Порт Redis | `6379` |
| `PAYMENT_SERVICE_URL` | URL сервиса платежей | `http://localhost:8081` |

### Переменные окружения Payment Service

| Переменная | Описание | Значение по умолчанию |
|------------|----------|----------------------|
| `SPRING_R2DBC_URL` | URL базы данных | `r2dbc:postgresql://localhost:5433/payment_db` |
| `SPRING_R2DBC_USERNAME` | Пользователь БД | `payment_user` |
| `SPRING_R2DBC_PASSWORD` | Пароль БД | `payment_password` |

## Особенности реализации

### Реактивное программирование

Проект использует Project Reactor для неблокирующей асинхронной обработки:
- Все операции с БД через R2DBC возвращают `Mono`/`Flux`
- Кеширование реализовано реактивно через `ReactiveRedisTemplate`
- HTTP-клиент для интеграции с payment-service использует `WebClient`

### Интеграция сервисов

Взаимодействие между market-app и payment-service:
1. OpenAPI спецификация в `api-contracts`
2. Автогенерация клиентского кода в market-app
3. Автогенерация серверного кода в payment-service
4. Реактивный WebClient для HTTP-запросов

### Обработка ошибок

Централизованная обработка исключений через `@ControllerAdvice`:
- `ItemNotFoundException` → 404 Not Found
- `EmptyCartException` → 400 Bad Request
- `PaymentOperationException` → 400 Bad Request
- Прочие ошибки → 500 Internal Server Error

### Логирование

Все сервисы имеют подробное логирование:
- **DEBUG** - параметры методов, результаты операций, cache HIT/MISS
- **INFO** - бизнес-события (создание заказов, платежей)
- **WARN** - ожидаемые ошибки (товар не найден)
- **ERROR** - неожиданные ошибки

## Разработка

### Добавление нового микросервиса

1. Создать модуль в корневом pom.xml
2. Добавить OpenAPI спецификацию в `api-contracts`
3. Настроить генерацию кода через `openapi-generator-maven-plugin`
4. Создать Dockerfile
5. Добавить сервис в docker-compose.yml

### Git workflow

Проект следует GitFlow:
- `main` - стабильная ветка для продакшена
- `sprint_*` - ветки для разработки спринтов
- Микрокоммиты для каждой значимой функции

## Лицензия

Учебный проект Яндекс.Практикум
