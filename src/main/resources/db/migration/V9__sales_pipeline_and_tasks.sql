-- ============================================================
-- Воронки продаж, этапы, причины проигрыша
-- ============================================================
CREATE TABLE pipelines (
    id               UUID PRIMARY KEY,
    tenant_id        UUID         NOT NULL REFERENCES organizations (id),
    name             VARCHAR(255) NOT NULL,
    is_default       BOOLEAN      NOT NULL DEFAULT FALSE,
    lost_reason_mode VARCHAR(20)  NOT NULL DEFAULT 'OPTIONAL',
    is_archived      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at       TIMESTAMP WITHOUT TIME ZONE
);
-- Ровно одна воронка по умолчанию на организацию
CREATE UNIQUE INDEX uq_pipelines_default ON pipelines (tenant_id) WHERE is_default;

CREATE TABLE pipeline_stages (
    id          UUID PRIMARY KEY,
    tenant_id   UUID         NOT NULL REFERENCES organizations (id),
    pipeline_id UUID         NOT NULL REFERENCES pipelines (id),
    name        VARCHAR(255) NOT NULL,
    position    INTEGER      NOT NULL,
    -- Удалённый этап архивируется: на него ссылается история лидов
    is_archived BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX idx_pipeline_stages_pipeline ON pipeline_stages (pipeline_id, position);

CREATE TABLE lost_reasons (
    id          UUID PRIMARY KEY,
    tenant_id   UUID         NOT NULL REFERENCES organizations (id),
    pipeline_id UUID         NOT NULL REFERENCES pipelines (id),
    name        VARCHAR(255) NOT NULL,
    position    INTEGER      NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX idx_lost_reasons_pipeline ON lost_reasons (pipeline_id, position);

-- Воронка по умолчанию для уже существующих организаций.
-- Те же значения создаёт PipelineService.createDefault для новых организаций.
INSERT INTO pipelines (id, tenant_id, name, is_default, lost_reason_mode, created_at)
SELECT gen_random_uuid(), id, 'Продажи', TRUE, 'OPTIONAL', NOW() FROM organizations;

INSERT INTO pipeline_stages (id, tenant_id, pipeline_id, name, position, created_at)
SELECT gen_random_uuid(), p.tenant_id, p.id, s.name, s.position, NOW()
FROM pipelines p
CROSS JOIN (VALUES ('Новое обращение', 0), ('Взят в работу', 1),
                   ('Пробное занятие', 2), ('Принимает решение', 3)) AS s (name, position);

INSERT INTO lost_reasons (id, tenant_id, pipeline_id, name, position, created_at)
SELECT gen_random_uuid(), p.tenant_id, p.id, r.name, r.position, NOW()
FROM pipelines p
CROSS JOIN (VALUES ('Дорого', 0), ('Не подошло расписание', 1), ('Не выходит на связь', 2),
                   ('Выбрал другую школу', 3), ('Другое', 4)) AS r (name, position);

-- ============================================================
-- Лиды: воронка, статус, итог сделки, связь с карточкой человека
-- ============================================================
ALTER TABLE leads
    ADD COLUMN pipeline_id    UUID REFERENCES pipelines (id),
    ADD COLUMN status         VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN lost_reason_id UUID REFERENCES lost_reasons (id),
    ADD COLUMN lost_comment   TEXT,
    ADD COLUMN person_id      UUID REFERENCES persons (id),
    ADD COLUMN closed_at      TIMESTAMP WITHOUT TIME ZONE;

-- Существующие лиды (stage_id был заглушкой) — в первый этап воронки по умолчанию
UPDATE leads l
SET pipeline_id = p.id,
    stage_id    = (SELECT st.id FROM pipeline_stages st WHERE st.pipeline_id = p.id ORDER BY st.position LIMIT 1)
FROM pipelines p
WHERE p.tenant_id = l.tenant_id AND p.is_default;

ALTER TABLE leads ALTER COLUMN pipeline_id SET NOT NULL;
ALTER TABLE leads ALTER COLUMN is_archived SET NOT NULL;
ALTER TABLE leads ADD CONSTRAINT fk_leads_stage FOREIGN KEY (stage_id) REFERENCES pipeline_stages (id);
CREATE INDEX idx_leads_pipeline_stage ON leads (pipeline_id, stage_id);
CREATE INDEX idx_leads_tenant_phone ON leads (tenant_id, phone_number);
CREATE INDEX idx_leads_tenant_messenger ON leads (tenant_id, messenger_id);

-- История лида: этапы, ответственный, итог. Только добавление записей.
CREATE TABLE lead_events (
    id             UUID PRIMARY KEY,
    tenant_id      UUID        NOT NULL REFERENCES organizations (id),
    lead_id        UUID        NOT NULL REFERENCES leads (id),
    type           VARCHAR(30) NOT NULL,
    from_stage_id  UUID REFERENCES pipeline_stages (id),
    to_stage_id    UUID REFERENCES pipeline_stages (id),
    manager_id     UUID REFERENCES users (id),
    lost_reason_id UUID REFERENCES lost_reasons (id),
    comment        TEXT,
    actor_id       UUID REFERENCES users (id),
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX idx_lead_events_lead ON lead_events (lead_id, created_at);

-- ============================================================
-- Задачи и напоминания
-- ============================================================
CREATE TABLE tasks (
    id           UUID PRIMARY KEY,
    tenant_id    UUID         NOT NULL REFERENCES organizations (id),
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    due_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    assignee_id  UUID         NOT NULL REFERENCES users (id),
    created_by   UUID         NOT NULL REFERENCES users (id),
    lead_id      UUID REFERENCES leads (id),
    person_id    UUID REFERENCES persons (id),
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    completed_by UUID REFERENCES users (id),
    created_at   TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at   TIMESTAMP WITHOUT TIME ZONE
);
CREATE INDEX idx_tasks_assignee_status_due ON tasks (tenant_id, assignee_id, status, due_at);
CREATE INDEX idx_tasks_lead ON tasks (lead_id);
