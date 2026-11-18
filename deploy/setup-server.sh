#!/usr/bin/env bash
# Первичная настройка сервера Ubuntu (Oracle Cloud Always Free и любой другой VPS).
#
#   cd school_crm/deploy && sudo bash setup-server.sh
#
# Что делает:
#   1. ставит Docker (если его нет);
#   2. открывает порты 80/443 в firewall образа Oracle (iptables);
#   3. создаёт deploy/.env со случайными паролями и доменом crm.<IP>.sslip.io (если .env ещё нет).
# Запускать повторно безопасно.
set -euo pipefail

cd "$(dirname "$0")"

if [[ $EUID -ne 0 ]]; then
  echo "Запустите через sudo: sudo bash setup-server.sh" >&2
  exit 1
fi

# ---------- 1. Docker ----------
if ! command -v docker >/dev/null 2>&1; then
  echo "==> Устанавливаю Docker"
  curl -fsSL https://get.docker.com | sh
fi
if [[ -n "${SUDO_USER:-}" ]]; then
  usermod -aG docker "$SUDO_USER" || true
fi
systemctl enable --now docker >/dev/null

# ---------- 2. Firewall ----------
# В образах Ubuntu от Oracle iptables по умолчанию отклоняет всё, кроме SSH.
if command -v iptables >/dev/null 2>&1; then
  for port in 80 443; do
    if ! iptables -C INPUT -p tcp --dport "$port" -m state --state NEW -j ACCEPT 2>/dev/null; then
      echo "==> Открываю порт $port"
      iptables -I INPUT 6 -p tcp --dport "$port" -m state --state NEW -j ACCEPT 2>/dev/null \
        || iptables -I INPUT -p tcp --dport "$port" -m state --state NEW -j ACCEPT
    fi
  done
  if command -v netfilter-persistent >/dev/null 2>&1; then
    netfilter-persistent save >/dev/null
  fi
fi

# ---------- 3. Секреты и домен ----------
if [[ ! -f .env ]]; then
  echo "==> Создаю .env"
  ip="$(curl -fsS https://api.ipify.org || true)"
  if [[ -z "$ip" ]]; then
    read -rp "Публичный IP сервера: " ip
  fi
  random() { openssl rand -hex "$1"; }
  admin_password="$(random 8)"
  cat > .env <<EOF
CRM_DOMAIN=crm.${ip}.sslip.io
POSTGRES_PASSWORD=$(random 16)
CRM_JWT_SECRET=$(random 32)
CRM_ADMIN_EMAIL=admin@crm.local
CRM_ADMIN_PASSWORD=${admin_password}
CRM_ADMIN_NAME="Администратор CRM"
EOF
  chmod 600 .env
  if [[ -n "${SUDO_USER:-}" ]]; then chown "$SUDO_USER" .env; fi
  echo
  echo "Сайт:                https://crm.${ip}.sslip.io"
  echo "Суперадмин платформы: admin@crm.local / ${admin_password}"
  echo "(сохранено в deploy/.env)"
else
  echo "==> .env уже есть — не трогаю"
fi

echo
echo "Готово. Дальше:"
echo "  docker compose -f docker-compose.prod.yml up -d --build"
