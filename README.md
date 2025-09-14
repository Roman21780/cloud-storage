# Cloud Storage Service

REST-сервис для облачного хранения файлов с авторизацией и управлением файлами

## Структура проекта
cloud-storage/
├── src/main/
│   ├── java/com/example/cloudstorage/
│   │   ├── CloudStorageApplication.java
│   │   ├── config/
│   │   │   └── SecurityConfig.java
│   │   │   
│   │   ├── controller/
│   │   │   └── CloudStorageController.java
│   │   ├── dto/
│   │   │   ├── AuthRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   ├── ErrorResponse.java
│   │   │   ├── FileResponse.java
|   |   |   ├── RegisterRequest.java
|   |   |   ├── RegisterResponse.java
│   │   │   └── RenameRequest.java
│   │   ├── entity/
│   │   │   ├── FileEntity.java
│   │   │   └── UserEntity.java
│   │   ├── exception/
│   │   │   └── FileStorageException.java
│   │   │   
│   │   ├── repository/
│   │   │   ├── FileRepository.java
│   │   │   └── UserRepository.java
│   │   └── service/
│   │       ├── FileStorageService.java
│   │       ├── TokenService.java
│   │       └── UserService.java
│   │    
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

## Создаем базу данных и пользователя PostgreSQL

psql -U postgres

## В интерфейсе psql выполните:
CREATE DATABASE clouddb;
CREATE USER clouduser WITH PASSWORD 'cloudpass';
GRANT ALL PRIVILEGES ON DATABASE clouddb TO clouduser;
ALTER DATABASE clouddb OWNER TO clouduser;
\q

## Пересоздать базу

DROP DATABASE clouddb;
CREATE DATABASE clouddb OWNER clouduser;

## Запуск приложения

### Сборка и запуск через Docker Compose

bash
# Очистка и сборка приложения
./gradlew clean build

## Запуск приложения

./gradlew bootRun


# Запуск всех сервисов
docker-compose up -d


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
