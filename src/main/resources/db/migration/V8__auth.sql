-- Сотрудники школы: ФИО и время последнего входа
ALTER TABLE users
    ADD COLUMN full_name     VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN last_login_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE users ALTER COLUMN full_name DROP DEFAULT;
ALTER TABLE users ALTER COLUMN is_blocked SET NOT NULL;

-- Преподаватель группы — сотрудник этой же школы
ALTER TABLE groups
    ADD CONSTRAINT fk_groups_teacher FOREIGN KEY (teacher_id) REFERENCES users (id);

-- Представители CRM (суперадминистраторы). Не принадлежат ни одной школе.
CREATE TABLE platform_admins (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    is_blocked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at    TIMESTAMP WITHOUT TIME ZONE
);

-- Журнал успешных и неуспешных входов. Неизменяемый: только INSERT.
-- tenant_id пуст для входа в platform API и для неизвестного поддомена.
CREATE TABLE login_attempts (
    id             UUID PRIMARY KEY,
    tenant_id      UUID REFERENCES organizations (id),
    user_id        UUID REFERENCES users (id),
    platform_admin BOOLEAN      NOT NULL DEFAULT FALSE,
    email          VARCHAR(255) NOT NULL,
    success        BOOLEAN      NOT NULL,
    failure_reason VARCHAR(50),
    ip_address     VARCHAR(64),
    user_agent     VARCHAR(512),
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_login_attempts_tenant_time ON login_attempts (tenant_id, created_at DESC);
