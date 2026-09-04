-- Единая таблица карточки человека
CREATE TABLE persons (
                         id UUID PRIMARY KEY,
                         tenant_id UUID NOT NULL REFERENCES organizations(id),
                         first_name VARCHAR(255) NOT NULL,
                         last_name VARCHAR(255),
                         phone_number VARCHAR(50),
                         messenger_id VARCHAR(100),
                         is_student BOOLEAN DEFAULT FALSE,
                         is_payer BOOLEAN DEFAULT FALSE,
                         is_archived BOOLEAN DEFAULT FALSE,
                         created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
                         updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- Таблица связей (родства) между учениками и их законными представителями
CREATE TABLE student_representatives (
                                         student_id UUID NOT NULL REFERENCES persons(id),
                                         representative_id UUID NOT NULL REFERENCES persons(id),
                                         PRIMARY KEY (student_id, representative_id)
);