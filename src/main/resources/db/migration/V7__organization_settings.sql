-- Настройки организации (школы) для пилотного MVP
ALTER TABLE organizations
    ADD COLUMN status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN timezone        VARCHAR(64)  NOT NULL DEFAULT 'Asia/Almaty',
    ADD COLUMN industry_module VARCHAR(50)  NOT NULL DEFAULT 'LANGUAGE_SCHOOL';

-- Индексы по tenant_id: каждый запрос к данным школы фильтруется по организации
CREATE INDEX idx_users_tenant         ON users (tenant_id);
CREATE INDEX idx_leads_tenant         ON leads (tenant_id);
CREATE INDEX idx_persons_tenant       ON persons (tenant_id);
CREATE INDEX idx_groups_tenant        ON groups (tenant_id);
CREATE INDEX idx_lessons_tenant       ON lessons (tenant_id);
CREATE INDEX idx_attendance_tenant    ON attendance (tenant_id);
CREATE INDEX idx_subscriptions_tenant ON subscriptions (tenant_id);
