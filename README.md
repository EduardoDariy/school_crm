# school_crm

CRM для частной языковой школы: Spring Boot 4 + PostgreSQL (backend), Angular (`school-crm-ui`).

## Запуск

```bash
docker compose --profile app up -d --build   # db + backend (:8080) + frontend (:4200, /api проксируется)
docker compose up -d                         # только db — для запуска backend из IDE
./mvnw test                                  # тесты; нужен Docker (Testcontainers)
```

## Доступ (локальная разработка)

Значения заданы в `docker-compose.yaml` и предназначены **только для разработки**:

| Переменная           | Назначение                                        |
|----------------------|---------------------------------------------------|
| `CRM_JWT_SECRET`     | ключ подписи токенов, не короче 32 символов       |
| `CRM_ADMIN_EMAIL`    | email первого суперадминистратора (`admin@crm.local`) |
| `CRM_ADMIN_PASSWORD` | его пароль (`admin12345`)                         |

Суперадминистратор создаётся при старте, только если его ещё нет. При запуске backend из IDE
задайте эти переменные в конфигурации запуска; без `CRM_JWT_SECRET` ключ генерируется случайно
и токены сбрасываются при перезапуске.

## Как подключить школу

1. `POST /api/platform/auth/login` `{email, password}` — токен представителя CRM.
2. `POST /api/platform/organizations` `{name, subdomain}` — организация.
3. `POST /api/platform/organizations/{id}/admins` `{email, fullName, password}` — администратор школы.
4. `POST /api/auth/login` `{subdomain, email, password}` — вход сотрудника школы.
5. Далее запросы с заголовком `Authorization: Bearer <accessToken>`; сотрудников заводит
   администратор школы через `/api/users`.

Роли сотрудников: `ADMIN`, `MANAGER`, `TEACHER`, `FINANCE`. Данные каждой школы изолированы:
организация определяется по токену, фильтрация выполняется Hibernate (`@TenantId`).

## Тестовые данные

Скрипт `scripts/seed-demo.mjs` наполняет школу через API: 5 сотрудников (менеджеры,
преподаватели, финансист), 28 лидов на разных этапах (с историей, продажами, отказами, архивом),
задачи (просроченные, на сегодня, выполненные), 3 группы с расписанием, абонементами и
посещаемостью, должников. Node.js на хосте не нужен:

```bash
docker run --rm -v "$PWD/scripts:/s" node:22-alpine node /s/seed-demo.mjs
```

Школа должна существовать (по умолчанию `demo`, админ `admin@demo.kz` / `demo12345`; можно
переопределить `CRM_API`, `CRM_SUBDOMAIN`, `CRM_EMAIL`, `CRM_PASSWORD`). Пароль созданных
сотрудников — `demo12345`. Повторный запуск ничего не делает.

## Документация API (Swagger)

http://localhost:8080/swagger-ui.html — вверху справа переключатель «Школа» / «Платформа».
Выполните `login`, скопируйте `accessToken`, нажмите **Authorize** и вставьте токен.
В production отключается переменными `SPRINGDOC_API_DOCS_ENABLED=false` и `SPRINGDOC_SWAGGER_UI_ENABLED=false`.
