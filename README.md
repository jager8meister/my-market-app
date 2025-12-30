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
- Spring Security 6 (аутентификация и авторизация)
- Spring Security OAuth2 Client (OAuth2 клиент для межсервисного взаимодействия)
- Spring Security OAuth2 Resource Server (валидация JWT токенов)
- Keycloak 23.0 (сервер авторизации)
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

## Безопасность и аутентификация

Проект использует многоуровневую систему безопасности на основе **Spring Security** и **Keycloak**.

### Архитектура безопасности

```
┌──────────────┐         ┌─────────────────┐         ┌──────────────────┐
│   Browser    │         │   Market App    │         │ Payment Service  │
│              │         │                 │         │                  │
│ Login Form   │────────▶│ Spring Security │         │ OAuth2 Resource  │
│              │ User +  │ Form Login      │         │ Server (JWT)     │
│              │ Password│                 │         │                  │
└──────────────┘         └────────┬────────┘         └────────▲─────────┘
                                  │                           │
                                  │ OAuth2 Client             │
                                  │ Credentials Flow          │ JWT Token
                                  │                           │
                         ┌────────▼───────────────────────────┴─────────┐
                         │           Keycloak 23.0                      │
                         │  Authorization Server (OAuth2 + OpenID)      │
                         │  - Issues JWT tokens                         │
                         │  - Validates credentials                     │
                         │  - Realm: my-market                          │
                         └──────────────────────────────────────────────┘
```

### 1. Аутентификация пользователей (Market App)

**Механизм**: Spring Security Form-Based Login с BCrypt шифрованием паролей

**Тестовые пользователи**:
```
Логин: user1  | Пароль: password1 | Баланс: 1 000 000 ₽
Логин: user2  | Пароль: password2 | Баланс: 500 000 ₽
Логин: user3  | Пароль: password3 | Баланс: 750 000 ₽
```

**Доступ**:
- Все защищенные страницы требуют аутентификации
- После логина пользователь перенаправляется на главную страницу
- Сессия хранится в памяти приложения

### 2. OAuth2 межсервисное взаимодействие

**Механизм**: OAuth2 Client Credentials Flow

Market App → Keycloak → Payment Service:

1. **Market App** автоматически получает JWT токен от Keycloak при запросах к Payment Service
2. **Keycloak** выдает access token на основе client credentials
3. **Payment Service** валидирует JWT токен и проверяет подпись

**Конфигурация OAuth2 клиента** (market-app):
```properties
spring.security.oauth2.client.registration.market-app.client-id=market-app-client
spring.security.oauth2.client.registration.market-app.client-secret=market-app-secret
spring.security.oauth2.client.registration.market-app.authorization-grant-type=client_credentials
spring.security.oauth2.client.registration.market-app.scope=openid,profile
spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:8180/realms/my-market
```

**Конфигурация OAuth2 Resource Server** (payment-service):
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8180/realms/my-market
```

### 3. Защищенные эндпоинты

**Market App**:
- `/login` - публичный эндпоинт для формы входа
- `/logout` - полный OIDC logout через Keycloak (см. детали ниже)
- Все остальные страницы (`/`, `/cart/*`, `/orders/*`) - требуют аутентификации

**Payment Service**:
- `/actuator/health` - публичный (для health checks)
- `/api/**` - требуется валидный JWT токен от Keycloak
- Все остальные эндпоинты - требуется аутентификация

### 4. OIDC Logout

Приложение реализует полный **OpenID Connect Logout** через Keycloak, который:
- Завершает локальную сессию в приложении
- Завершает SSO сессию в Keycloak
- Инвалидирует все токены
- Перенаправляет пользователя обратно на страницу входа

**Реализация:**

Создан кастомный `OAuth2LogoutConfig`, который оборачивает `ReactiveClientRegistrationRepository` и добавляет `end_session_endpoint` напрямую в provider metadata:

```java
@Configuration
public class OAuth2LogoutConfig {

    @Value("${keycloak.logout.endpoint}")
    private String keycloakLogoutEndpoint;

    @Bean
    @Primary
    public ReactiveClientRegistrationRepository enhancedClientRegistrationRepository(
            @Lazy ReactiveClientRegistrationRepository originalRepository) {
        // Добавляет end_session_endpoint для Keycloak
        // Использует @Lazy для разрыва циклической зависимости
    }
}
```

**Конфигурация:**
```properties
keycloak.logout.endpoint=http://localhost:8180/realms/my-market/protocol/openid-connect/logout
```

**Последовательность logout:**
1. Пользователь нажимает "Выйти" → POST `/logout`
2. `OidcClientInitiatedServerLogoutSuccessHandler` формирует URL для Keycloak
3. Redirect на `http://localhost:8180/realms/my-market/protocol/openid-connect/logout?id_token_hint=...&post_logout_redirect_uri=...`
4. Keycloak завершает сессию и инвалидирует токены
5. Redirect обратно на `/login?logout`

**Детали решения:** См. `OIDC-LOGOUT-SOLUTION.md`

### 5. Keycloak настройка

**Realm**: `my-market`

**Client настройки**:
- **Client ID**: `market-app-client`
- **Client Secret**: `market-app-secret`
- **Access Type**: Confidential
- **Service Accounts Enabled**: ON
- **Valid Redirect URIs**: `http://localhost:8080/*`

**Доступ к Keycloak Admin Console**:
```
URL: http://localhost:8180/admin
Username: admin
Password: admin
```

### 6. Тестирование безопасности

Созданы модульные и интеграционные тесты:

**Market App** (8 тестов):
- `SecurityConfigTest` - тесты Spring Security конфигурации
- `ReactiveUserDetailsServiceImplTest` - тесты загрузки пользователей
- `PaymentClientConfigTest` - тесты OAuth2 клиента

**Payment Service** (4 теста):
- `PaymentServiceTest` - тесты бизнес-логики платежей

Запуск тестов безопасности:
```bash
# Market App тесты
cd market-app
mvn test -Dtest=SecurityConfigTest,ReactiveUserDetailsServiceImplTest,PaymentClientConfigTest

# Payment Service тесты
cd payment-service
mvn test -Dtest=PaymentServiceTest
```

## Как запустить

### Вариант 1: Docker Compose (рекомендуется)

Запустить все сервисы (БД, Redis, Keycloak, приложения):

```bash
mvn clean package -DskipTests
docker-compose up --build
```

Приложения и сервисы будут доступны:
- **Market App**: http://localhost:8080 (требуется логин: user1/password1)
- **Payment Service**: http://localhost:8081
- **Keycloak Admin Console**: http://localhost:8180/admin (admin/admin)
- **Keycloak Realm**: http://localhost:8180/realms/my-market
- **Swagger UI Market App**: http://localhost:8080/swagger-ui.html
- **Swagger UI Payment Service**: http://localhost:8081/swagger-ui.html

**Проверка работоспособности**:
```bash
# Health check всех сервисов
curl http://localhost:8080/actuator/health  # Market App
curl http://localhost:8081/actuator/health  # Payment Service
curl http://localhost:8180/health/ready     # Keycloak
```

**Логин в Market App**:
1. Откройте http://localhost:8080
2. Введите логин: `user1` и пароль: `password1`
3. После успешного входа вы увидите баланс и каталог товаров

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

### Запуск всех тестов

Запуск всех unit-тестов в мультимодульном проекте:

```bash
# Из корневой директории проекта
mvn clean test

# Результаты:
# - market-app: 8+ тестов (включая Security и OAuth2)
# - payment-service: 4+ теста
```

### Запуск тестов по модулям

**Market App**:
```bash
cd market-app
mvn test

# Тесты безопасности:
mvn test -Dtest=SecurityConfigTest
mvn test -Dtest=ReactiveUserDetailsServiceImplTest
mvn test -Dtest=PaymentClientConfigTest

# Тесты бизнес-логики:
mvn test -Dtest=OrderServiceImplTest
```

**Payment Service**:
```bash
cd payment-service
mvn test

# Конкретный тест:
mvn test -Dtest=PaymentServiceTest
```

### Запуск интеграционных тестов

```bash
mvn test -P integration-test
```

Интеграционные тесты используют Testcontainers для автоматического поднятия PostgreSQL и Redis.

### Тестирование OAuth2 интеграции

**End-to-end тест OAuth2 flow**:

1. Запустите все сервисы через Docker Compose
2. Проверьте получение JWT токена:
```bash
# Получить токен от Keycloak
TOKEN=$(curl -X POST http://localhost:8180/realms/my-market/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=market-app-client" \
  -d "client_secret=market-app-secret" \
  | jq -r '.access_token')

# Использовать токен для вызова Payment Service API
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/balances/1
```

3. Проверьте автоматическую передачу токена через Market App:
```bash
# Market App должен автоматически получать токен и передавать его в Payment Service
# Логин в браузере → видите баланс → значит OAuth2 работает
```

### Покрытие кода

Отчет JaCoCo генерируется при запуске тестов:

```bash
mvn clean test

# Отчеты по модулям:
# - market-app/target/site/jacoco/index.html
# - payment-service/target/site/jacoco/index.html
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
│   │       ├── config/          # Конфигурация (Redis, WebClient, Security, OAuth2)
│   │       ├── security/        # Spring Security (UserDetailsService)
│   │       ├── controllers/     # REST и View контроллеры
│   │       ├── client/          # PaymentClient с OAuth2
│   │       ├── dto/            # DTO запросов/ответов
│   │       ├── entity/         # R2DBC сущности (UserEntity, ItemEntity, OrderEntity)
│   │       ├── exception/      # Обработка ошибок
│   │       ├── mapper/         # MapStruct маппинг
│   │       ├── repository/     # Spring Data R2DBC репозитории
│   │       └── service/        # Бизнес-логика, кеширование
│   └── src/test/            # Unit и интеграционные тесты
│       └── java/
│           └── ru/yandex/practicum/mymarket/
│               ├── config/      # PaymentClientConfigTest
│               ├── security/    # SecurityConfigTest, ReactiveUserDetailsServiceImplTest
│               └── service/     # OrderServiceImplTest
├── payment-service/         # Сервис платежей
│   ├── src/main/java/
│   │   └── ru/yandex/practicum/payment/
│   │       ├── config/         # SecurityConfig (OAuth2 Resource Server)
│   │       ├── controller/     # BalanceController, REST контроллеры
│   │       ├── entity/        # Сущности платежей
│   │       ├── mapper/        # MapStruct маппинг
│   │       ├── repository/    # Репозитории
│   │       └── service/       # Логика обработки платежей (PaymentService, BalanceService)
│   └── src/test/            # Тесты (PaymentServiceTest)
├── docker-compose.yml       # Docker Compose (Postgres, Redis, Keycloak, Apps)
└── pom.xml                 # Parent POM (мультимодульный проект)
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
| `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_KEYCLOAK_ISSUER_URI` | Keycloak issuer URI | `http://localhost:8180/realms/my-market` |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MARKET_APP_CLIENT_ID` | OAuth2 Client ID | `market-app-client` |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MARKET_APP_CLIENT_SECRET` | OAuth2 Client Secret | `market-app-secret` |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MARKET_APP_AUTHORIZATION_GRANT_TYPE` | OAuth2 Grant Type | `client_credentials` |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_MARKET_APP_SCOPE` | OAuth2 Scopes | `openid,profile` |

### Переменные окружения Payment Service

| Переменная | Описание | Значение по умолчанию |
|------------|----------|----------------------|
| `SPRING_R2DBC_URL` | URL базы данных | `r2dbc:postgresql://localhost:5433/payment_db` |
| `SPRING_R2DBC_USERNAME` | Пользователь БД | `payment_user` |
| `SPRING_R2DBC_PASSWORD` | Пароль БД | `payment_password` |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | Keycloak issuer URI для валидации JWT | `http://keycloak:8180/realms/my-market` |

## Особенности реализации

### Реактивное программирование

Проект использует Project Reactor для неблокирующей асинхронной обработки:
- Все операции с БД через R2DBC возвращают `Mono`/`Flux`
- Кеширование реализовано реактивно через `ReactiveRedisTemplate`
- HTTP-клиент для интеграции с payment-service использует `WebClient`

### Интеграция сервисов

Взаимодействие между market-app и payment-service с использованием OAuth2:

1. **OpenAPI спецификация** в модуле `api-contracts`
2. **Автогенерация клиентского кода** в market-app через `openapi-generator-maven-plugin`
3. **Автогенерация серверного кода** в payment-service через `openapi-generator-maven-plugin`
4. **OAuth2 Protected WebClient** для HTTP-запросов:
   ```java
   @Bean
   public WebClient paymentWebClient(
           WebClient.Builder builder,
           ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {

       ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
               new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

       oauth2.setDefaultClientRegistrationId("market-app");

       return builder.filter(oauth2).build();
   }
   ```

**OAuth2 Flow**:
1. Market App делает запрос к Payment Service через `PaymentClient`
2. `ServerOAuth2AuthorizedClientExchangeFilterFunction` перехватывает запрос
3. Если токена нет или он истек → автоматически запрашивает новый токен у Keycloak
4. Keycloak выдает JWT токен (Client Credentials Flow)
5. Токен автоматически добавляется в заголовок `Authorization: Bearer <token>`
6. Payment Service валидирует JWT токен через Spring Security OAuth2 Resource Server
7. Запрос обрабатывается, если токен валиден

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
