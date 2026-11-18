# Бесплатный хостинг: Render + Neon

Всё настраивается в браузере, без серверов и SSH. Обновления выкатываются сами при push в `main`.

```
Браузер → school-crm.onrender.com (интерфейс, статический сайт)
              └─ /api/* → school-crm-api.onrender.com (backend, Docker) → Neon (PostgreSQL)
```

**Ограничения бесплатных тарифов:**
- backend засыпает после 15 минут без запросов; первый вход после этого ждёт **1–2 минуты**
  (интерфейс открывается сразу, а вход покажет «Вход…», пока backend просыпается);
- 750 часов работы backend в месяц — хватает, если он один;
- база Neon: 0,5 ГБ, бессрочно; засыпает при простое и просыпается за секунду.

## 1. База данных — Neon

Проект привязан к репозиторию через Neon CLI (`neon.ts`, локальный файл `.neon`).
Строки подключения получаются командой `neon deploy` — она кладёт их в `.env.local`
(в git не попадает):

| Переменная | Что это | Кому |
|---|---|---|
| `DATABASE_URL` | через пул PgBouncer (хост с `-pooler`) | запросы приложения |
| `DATABASE_URL_UNPOOLED` | напрямую | миграции Flyway (им нужны блокировки сессии) |

Без CLI: консоль Neon → проект → **Connect**; строка с включённым *Connection pooling* —
`DATABASE_URL`, с выключенным — `DATABASE_URL_UNPOOLED`. Обе вставляются как есть, backend
сам переводит их в формат JDBC.

Регион базы — **aws-us-east-2**, поэтому backend в `render.yaml` размещён в **Ohio**: рядом
с базой каждый запрос быстрее (из Европы каждый запрос пересекал бы океан).

## 2. Приложение — Render

1. Зарегистрируйтесь на <https://render.com> через **GitHub**.
2. **New → Blueprint** → подключите репозиторий `school_crm` (кнопка *Connect GitHub*,
   доступ можно дать только к нему).
3. Render прочитает `render.yaml` и покажет два сервиса: `school-crm-api` и `school-crm`.
   Заполните переменные:

   | Переменная | Значение |
   |---|---|
   | `DATABASE_URL` | из `.env.local`, без кавычек |
   | `DATABASE_URL_UNPOOLED` | из `.env.local`, без кавычек |
   | `CRM_ADMIN_EMAIL` | email суперадминистратора платформы, например `admin@crm.local` |
   | `CRM_ADMIN_PASSWORD` | придумайте надёжный пароль (не короче 8 символов) |

   Ключ подписи токенов (`CRM_JWT_SECRET`) Render сгенерирует сам.
4. **Apply** и ждите: первая сборка backend — 5–10 минут, интерфейса — 2–3 минуты.
   Ход сборки — в *Logs* каждого сервиса; backend готов, когда в логе
   `Started CrmApplication`.

### Проверьте адрес backend

Откройте сервис `school-crm-api` — вверху его адрес. Если он **не**
`https://school-crm-api.onrender.com` (имя было занято и Render добавил суффикс):
сервис `school-crm` → **Redirects/Rewrites** → в правиле `/api/*` замените адрес на настоящий
(и то же самое в `render.yaml`, иначе при следующей синхронизации Blueprint правило вернётся).

## 3. Тестовые данные

В текущей базе Neon демо-школа `demo` уже заполнена — этот шаг нужен только для новой, пустой
базы. С вашего компьютера (PowerShell, из папки проекта), подставив email и пароль из шага 2:

```powershell
docker run --rm -e CRM_API=https://school-crm-api.onrender.com `
  -e CRM_PLATFORM_EMAIL=admin@crm.local -e CRM_PLATFORM_PASSWORD=<пароль> `
  -v "${PWD}/scripts:/s" node:22-alpine node /s/seed-demo.mjs
```

Если backend спит, первый запрос подождёт его пробуждения (до 2 минут).

## 4. Готово

Откройте `https://school-crm.onrender.com`: школа `demo`, `admin@demo.kz` / `demo12345`.
Swagger — на адресе backend: `https://school-crm-api.onrender.com/swagger-ui.html`.

> Тестовые пароли `demo12345` публичны (они в репозитории) — настоящие данные под ними не храните.

## Если что-то не так

| Симптом | Что проверить |
|---|---|
| «Сервер недоступен» при входе | backend спит или ещё собирается — подождите 2 минуты; *Logs* сервиса `school-crm-api` |
| В логе backend ошибка подключения к базе | обе строки скопированы целиком и без кавычек |
| Миграция падает с `prepared statement` или блокировкой | задан `DATABASE_URL_UNPOOLED` (хост **без** `-pooler`) |
| 404 на `/api/...` | адрес backend в правиле Rewrite (см. «Проверьте адрес backend») |
| Backend перезапускается по кругу | *Logs*: если `OutOfMemoryError` — напишите, уменьшим потребление памяти |
