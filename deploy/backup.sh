#!/usr/bin/env bash
# Резервная копия базы в deploy/backups (хранятся последние 14).
#
#   bash backup.sh
#
# Ежедневно в 03:00 (crontab -e):
#   0 3 * * * cd /home/ubuntu/school_crm/deploy && bash backup.sh >> backups/backup.log 2>&1
#
# Восстановление:
#   gunzip -c backups/crm-ДАТА.sql.gz | docker compose -f docker-compose.prod.yml exec -T db psql -U crm_user -d crm_db
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p backups
file="backups/crm-$(date +%Y%m%d-%H%M%S).sql.gz"
docker compose -f docker-compose.prod.yml exec -T db pg_dump -U crm_user -d crm_db --clean --if-exists | gzip > "$file"
ls -1t backups/crm-*.sql.gz | tail -n +15 | xargs -r rm --
echo "$(date '+%F %T') сохранено: $file"
