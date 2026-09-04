CREATE TABLE lessons (
                         id UUID PRIMARY KEY,
                         tenant_id UUID NOT NULL REFERENCES organizations(id),
                         group_id UUID NOT NULL REFERENCES groups(id),
                         start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                         end_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                         status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
                         topic VARCHAR(255),
                         created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
                         updated_at TIMESTAMP WITHOUT TIME ZONE
);