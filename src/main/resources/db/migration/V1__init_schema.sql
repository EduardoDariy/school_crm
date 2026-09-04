-- Таблица организаций (школ) для обеспечения многопользовательской SaaS-архитектуры
CREATE TABLE organizations (
                               id UUID PRIMARY KEY,
                               name VARCHAR(255) NOT NULL,
                               subdomain VARCHAR(255) UNIQUE NOT NULL,
                               created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
                               updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- Таблица сотрудников
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       tenant_id UUID NOT NULL REFERENCES organizations(id),
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       is_blocked BOOLEAN DEFAULT FALSE,
                       created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
                       updated_at TIMESTAMP WITHOUT TIME ZONE,
                       UNIQUE (tenant_id, email)
);

-- Таблица лидов
CREATE TABLE leads (
                       id UUID PRIMARY KEY,
                       tenant_id UUID NOT NULL REFERENCES organizations(id),
                       name VARCHAR(255) NOT NULL,
                       phone_number VARCHAR(50),
                       messenger_id VARCHAR(100),
                       source VARCHAR(100),
                       stage_id UUID NOT NULL,
                       manager_id UUID REFERENCES users(id),
                       is_archived BOOLEAN DEFAULT FALSE,
                       created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
                       updated_at TIMESTAMP WITHOUT TIME ZONE
);