# friend-microservice

## Описание проекта

**friend-microservice** — это микросервис, отвечающий за управление отношениями между пользователями в социальной сети.
Он предоставляет API для добавления в друзья, подписок, блокировок и получения рекомендаций.

Микросервис взаимодействует с другими сервисами через **Apache Kafka**, а данные хранятся в **PostgreSQL**.

## Стек технологий

- **Java** 17
- **Spring Boot** (Spring Security, Spring Data, JPA)
- **Apache Kafka** (для асинхронного взаимодействия между сервисами)
- **PostgreSQL** (основная база данных)
- **Docker** (контейнеризация)
- **JUnit, Mockito** (тестирование)
- **TeamCity** (CI/CD)

## Аутентификация

Для работы с API требуется JWT-аутентификация (Bearer Token).

## Основные эндпоинты

<!-- prettier-ignore --> <table> <thead> <tr> <th>Метод</th> <th>URL</th> <th>Описание</th> </tr> </thead> <tbody> <tr> <td><code>POST</code></td> <td><code>/api/v1/friends/{id}/request</code></td> <td>Отправить запрос в друзья</td> </tr> <tr> <td><code>PUT</code></td> <td><code>/api/v1/friends/{id}/approve</code></td> <td>Подтвердить запрос в друзья</td> </tr> <tr> <td><code>DELETE</code></td> <td><code>/api/v1/friends/{id}</code></td> <td>Удалить друга</td> </tr> <tr> <td><code>PUT</code></td> <td><code>/api/v1/friends/block/{id}</code></td> <td>Заблокировать пользователя</td> </tr> <tr> <td><code>PUT</code></td> <td><code>/api/v1/friends/unblock/{id}</code></td> <td>Разблокировать пользователя</td> </tr> <tr> <td><code>GET</code></td> <td><code>/api/v1/friends/recommendations</code></td> <td>Получить список рекомендаций (друзья друзей)</td> </tr> </tbody> </table>