CREATE TABLE attendance (
                            id UUID PRIMARY KEY,
                            tenant_id UUID NOT NULL REFERENCES organizations(id),
                            lesson_id UUID NOT NULL REFERENCES lessons(id),
                            student_id UUID NOT NULL REFERENCES persons(id),
                            status VARCHAR(50) NOT NULL,
                            grade INTEGER,
                            note TEXT,
                            created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
                            updated_at TIMESTAMP WITHOUT TIME ZONE,
                            UNIQUE(lesson_id, student_id) -- Один ученик может получить только одну запись на одном уроке
);