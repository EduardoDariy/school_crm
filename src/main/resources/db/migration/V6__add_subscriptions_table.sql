CREATE TABLE subscriptions (
                               id UUID PRIMARY KEY,
                               tenant_id UUID NOT NULL REFERENCES organizations(id),
                               student_id UUID NOT NULL REFERENCES persons(id),
                               group_id UUID REFERENCES groups(id),
                               lessons_total INTEGER NOT NULL,
                               lessons_remaining INTEGER NOT NULL,
                               is_active BOOLEAN DEFAULT TRUE,
                               created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
                               updated_at TIMESTAMP WITHOUT TIME ZONE
);