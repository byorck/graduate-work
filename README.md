# Сервис по созданию объявлений пользователей

## Описание проекта
Дипломный проект для создания платформы интернет-объявлений. Пользователи могут создавать объявления с фотографиями, просматривать объявления других пользователей, оставлять комментарии и управлять своими профилями.

## Технологический стек
- **Backend**: Spring Boot 3.5.5
- **База данных**: PostgreSQL
- **Безопасность**: Spring Security
- **Документация**: OpenAPI 3 (Swagger)
- **Миграции**: Liquibase
- **Сборка**: Maven

## Требования к окружению
- JDK 17 или выше
- Maven 3.6+
- PostgreSQL 12+
- Браузер с поддержкой JavaScript

## Настройка базы данных
Установите переменные окружения перед запуском:
- MY_DB=ads_database
- MY_NAME=ads_user
- MY_PASS=ads_password
  
## Миграции базы данных
Миграции управляются через Liquibase.

## Настройка конфигураций (application.properties)
```
server.port=8080
```
### Настройки PostgreSQL
```
spring.datasource.url=jdbc:postgresql://localhost:5432/${MY_DB}
spring.datasource.username=${MY_NAME}
spring.datasource.password=${MY_PASS}
spring.datasource.driver-class-name=org.postgresql.Driver
```
### Пути для хранения файлов
```
path.to.avatars.folder=./avatars
path.to.ads.folder=./ads
```
### Миграции базы данных
```
spring.liquibase.change-log=classpath:/liquibase/changelog-master.yaml
spring.liquibase.enabled=true
```
### Настройки Hibernate
```
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.show-sql=true
```
### Логирование
```
logging.level.org.hibernate.SQL=DEBUG
logging.level.liquibase=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
logging.level.org.springframework.security=DEBUG
```
## Тестирование
Запуск тестов:
```
mvn test
```
## Запуск приложения
### Сборка
```
mvn clean package
```
### Запуск
```
java -jar target/graduate-work.jar
```

## Доступ к приложению
### Веб-интерфейс
- Основное приложение: http://localhost:3000 (фронтенд)
- Бэкенд API: http://localhost:8080

### Документация API
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI спецификация: http://localhost:8080/v3/api-docs

## Основные функции API
### Аутентификация
- POST /register - регистрация нового пользователя
- POST /login - вход в систему
- POST /logout - выход из системы

### Пользователи
- GET /users/me - получение профиля текущего пользователя
- PATCH /users/me - обновление профиля
- POST /users/set_password - смена пароля
- PATCH /users/me/image - обновление аватара

### Объявления
- GET /ads - получение всех объявлений
- POST /ads - создание нового объявления
- GET /ads/me - получение объявлений текущего пользователя
- GET /ads/{id} - получение объявления по ID
- PATCH /ads/{id} - обновление объявления
- DELETE /ads/{id} - удаление объявления
- PATCH /ads/{id}/image - обновление изображения объявления

### Комментарии
- GET /ads/{id}/comments - получение комментариев объявления
- POST /ads/{id}/comments - добавление комментария
- DELETE /ads/{adId}/comments/{commentId} - удаление комментария
- PATCH /ads/{adId}/comments/{commentId} - обновление комментария

### Изображения
- GET /ads/{adId}/image - получение изображения объявления
- GET /users/{userId}/avatar - получение аватара пользователя

## Особенности реализации
- Хранение файлов - изображения сохраняются в файловой системе с путями в БД
- Генерация превью - автоматическое создание уменьшенных копий изображений
- Валидация данных - проверка входных данных на стороне сервера
- Обработка ошибок - единообразная система обработки исключений
- Логирование - детальное логирование для отладки

## Авторы
- Бурка Максим ([byorck](https://github.com/byorck))
