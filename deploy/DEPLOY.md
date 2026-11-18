# Развёртывание на Oracle Cloud (Always Free)

Итог: сайт `https://crm.<IP>.sslip.io` с настоящим HTTPS-сертификатом, работающий 24/7.
Домен покупать не нужно: [sslip.io](https://sslip.io) бесплатно превращает IP в доменное имя.

```
Интернет → Caddy (80/443, HTTPS) → frontend (nginx: интерфейс + /api) → backend → PostgreSQL
```

Наружу открыты только 80 и 443. База и backend доступны лишь внутри сервера.

## 1. Аккаунт Oracle Cloud

1. Зарегистрируйтесь на <https://signup.cloud.oracle.com>. Нужна банковская карта для проверки
   личности; на тарифе Always Free деньги не списываются.
2. **Home region** выбирается один раз навсегда — берите ближайший (например, Frankfurt или
   другой европейский). Бесплатные серверы создаются только в нём.

## 2. Сервер

**Menu → Compute → Instances → Create instance**:

| Параметр | Значение |
|---|---|
| Image | **Canonical Ubuntu 24.04** |
| Shape | **Ampere → VM.Standard.A1.Flex**, 2 OCPU, 12 GB (метка *Always Free-eligible*) |
| Networking | создать новую VCN с публичной подсетью, **Assign a public IPv4 address** — да |
| SSH keys | **Generate a key pair** → скачайте приватный ключ (`.key`) |

Нажмите **Create** и дождитесь статуса *Running*. Скопируйте **Public IP address**.

> Если пишет *Out of capacity* — в регионе временно нет свободных ARM-машин. Попробуйте другой
> *Availability domain* в том же окне или повторите позже (часто помогает через несколько часов).

## 3. Открыть порты 80 и 443 в облаке

На странице сервера: **Subnet → Security → Default Security List → Add Ingress Rules**
(в старом интерфейсе: *Virtual Cloud Network → Security Lists*):

| Source CIDR | IP Protocol | Destination Port |
|---|---|---|
| `0.0.0.0/0` | TCP | `80` |
| `0.0.0.0/0` | TCP | `443` |

Без этого сертификат не выпустится и сайт не откроется (firewall внутри самого сервера
настроит скрипт на шаге 5).

## 4. Подключиться по SSH

С Windows (PowerShell):

```powershell
ssh -i C:\путь\к\ssh-key.key ubuntu@<IP>
```

Если ssh ругается на права ключа: свойства файла → Безопасность → оставьте доступ только
своему пользователю.

## 5. Установка

На сервере:

```bash
git clone https://github.com/EduardoDariy/school_crm.git
cd school_crm/deploy
sudo bash setup-server.sh
```

Скрипт поставит Docker, откроет порты в firewall сервера и создаст `deploy/.env` со случайными
паролями. В конце он напечатает **адрес сайта и пароль суперадминистратора** — сохраните их
(они остаются в `deploy/.env`).

Запуск (первая сборка на сервере — 5–10 минут):

```bash
sudo docker compose -f docker-compose.prod.yml up -d --build
```

Проверка — откройте `https://crm.<IP>.sslip.io`. Сертификат выпускается при первом обращении,
это занимает до минуты.

## 6. Тестовые данные

Создаёт демо-школу `demo` и наполняет её так же, как локально:

```bash
cd ~/school_crm/deploy
set -a; . ./.env; set +a
sudo docker run --rm --network school-crm_default \
  -e CRM_API=http://backend:8080 \
  -e CRM_PLATFORM_EMAIL="$CRM_ADMIN_EMAIL" -e CRM_PLATFORM_PASSWORD="$CRM_ADMIN_PASSWORD" \
  -v "$PWD/../scripts:/s" node:22-alpine node /s/seed-demo.mjs
```

Вход: школа `demo`, `admin@demo.kz` / `demo12345` (остальные сотрудники — в выводе скрипта).

> Сайт публичный: тестовые пароли `demo12345` знает любой, кто видел этот репозиторий. Для
> показа это нормально, но настоящие данные школы под такими паролями не храните.

## Обновление до новой версии

```bash
cd ~/school_crm && git pull
cd deploy && sudo docker compose -f docker-compose.prod.yml up -d --build
```

Миграции базы (Flyway) применяются автоматически при старте backend.

## Обслуживание

```bash
cd ~/school_crm/deploy
sudo docker compose -f docker-compose.prod.yml ps              # состояние
sudo docker compose -f docker-compose.prod.yml logs -f backend # логи backend
sudo docker compose -f docker-compose.prod.yml restart backend # перезапуск
sudo bash backup.sh                                            # резервная копия базы
```

Ежедневный бэкап — `sudo crontab -e` и строка:

```
0 3 * * * cd /home/ubuntu/school_crm/deploy && bash backup.sh >> backups/backup.log 2>&1
```

Копии лежат на том же сервере; для надёжности периодически скачивайте их к себе
(`scp -i ключ ubuntu@<IP>:school_crm/deploy/backups/*.gz .`).

## Если что-то не работает

| Симптом | Что проверить |
|---|---|
| Сайт не открывается вообще | Ingress-правила 80/443 (шаг 3); `sudo iptables -L INPUT -n` — должны быть ACCEPT для 80 и 443 |
| Ошибка сертификата | `logs caddy`: Let's Encrypt должен достучаться до порта 80 снаружи |
| 502 Bad Gateway | backend ещё стартует (до минуты) или упал — `logs backend` |
| «Сервер недоступен» при входе | `ps` — все 4 контейнера должны быть *Up* |
