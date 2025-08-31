# Cloud Storage Service

REST-сервис для облачного хранения файлов с авторизацией и управлением файлами

## Структура проекта
cloud-storage/
├── src/main/
│   ├── java/com/example/cloudstorage/
│   │   ├── CloudStorageApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   └── WebConfig.java
│   │   ├── controller/
│   │   │   └── CloudStorageController.java
│   │   ├── dto/
│   │   │   ├── AuthRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   ├── ErrorResponse.java
│   │   │   ├── FileResponse.java
│   │   │   └── RenameRequest.java
│   │   ├── entity/
│   │   │   ├── FileEntity.java
│   │   │   └── UserEntity.java
│   │   ├── exception/
│   │   │   ├── FileStorageException.java
│   │   │   └── UserNotFoundException.java
│   │   ├── repository/
│   │   │   ├── FileRepository.java
│   │   │   └── UserRepository.java
│   │   ├── service/
│   │   │   ├── FileStorageService.java
│   │   │   ├── TokenService.java
│   │   │   └── UserService.java
│   │   └── util/
│   │       └── FileUtil.java
│   └── resources/
│       ├── application.yml
│       └── db/
│           └── migration/
│               └── V1__init.sql
├── Dockerfile
├── docker-compose.yml
├── build.gradle
└── README.md

## Технологии

- Spring Boot 3.5.5
- PostgreSQL
- Flyway (миграции)
- Gradle
- Docker & Docker Compose

## Запуск приложения

### Сборка и запуск через Docker Compose

bash
# Сборка приложения
./gradlew build

# Запуск всех сервисов
docker-compose up -d


# Ручной запуск
bash
# Сборка
./gradlew build

# Запуск базы данных
docker-compose up postgres -d

# Запуск приложения
java -jar build/libs/cloud-storage-1.0.0.jar


## API Endpoints

### Авторизация

POST /login - Вход в систему

POST /logout - Выход из системы

### Управление файлами

GET /file?filename={name} - Скачать файл

POST /file?filename={name} - Загрузить файл

DELETE /file?filename={name} - Удалить файл

PUT /file?filename={name} - Переименовать файл

GET /list?limit={n} - Список файлов

## Тестирование
Тестовые данные
Логин: testuser
Пароль: password123

# Unit тесты
./gradlew test

# Интеграционные тесты
./gradlew integrationTest

## Миграции базы данных
Миграции Flyway находятся в src/main/resources/db/migration/
