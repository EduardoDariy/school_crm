-- Таблица учебных групп
CREATE TABLE groups (
                        id UUID PRIMARY KEY,
                        tenant_id UUID NOT NULL REFERENCES organizations(id),
                        name VARCHAR(255) NOT NULL,
                        teacher_id UUID,
                        course_id UUID,
                        is_active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
                        updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- Таблица для связи "Многие-ко-многим": распределение учеников по группам
CREATE TABLE student_groups (
                                student_id UUID NOT NULL REFERENCES persons(id),
                                group_id UUID NOT NULL REFERENCES groups(id),
                                PRIMARY KEY (student_id, group_id)
);