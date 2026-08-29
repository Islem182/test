-- DGI Online Services Platform - Database Schema (PostgreSQL 16)

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Insert Default System Roles
INSERT INTO roles (name) VALUES 
('TAXPAYER'),
('AGENT'),
('ADMIN')
ON CONFLICT (name) DO NOTHING;

-- 3. User Roles Join Table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- 4. Taxpayers Table
CREATE TABLE IF NOT EXISTS taxpayers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    cin VARCHAR(20) UNIQUE,
    passport VARCHAR(30) UNIQUE,
    birth_date DATE,
    address TEXT,
    profile_picture VARCHAR(255),
    CONSTRAINT fk_taxpayers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 5. Tax Applications Table
CREATE TABLE IF NOT EXISTS tax_applications (
    id BIGSERIAL PRIMARY KEY,
    taxpayer_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_applications_taxpayer FOREIGN KEY (taxpayer_id) REFERENCES taxpayers (id) ON DELETE CASCADE
);

-- 6. Activities Table
CREATE TABLE IF NOT EXISTS activities (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    business_name VARCHAR(150) NOT NULL,
    business_type VARCHAR(100),
    sector VARCHAR(100),
    address TEXT,
    start_date DATE,
    CONSTRAINT fk_activities_application FOREIGN KEY (application_id) REFERENCES tax_applications (id) ON DELETE CASCADE
);

-- 7. Documents Table
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    upload_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    verification_status VARCHAR(30) DEFAULT 'PENDING',
    CONSTRAINT fk_documents_application FOREIGN KEY (application_id) REFERENCES tax_applications (id) ON DELETE CASCADE
);

-- 8. Fiscal Numbers Table
CREATE TABLE IF NOT EXISTS fiscal_numbers (
    id BIGSERIAL PRIMARY KEY,
    taxpayer_id BIGINT NOT NULL UNIQUE,
    fiscal_number VARCHAR(50) NOT NULL UNIQUE,
    generated_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fiscal_numbers_taxpayer FOREIGN KEY (taxpayer_id) REFERENCES taxpayers (id) ON DELETE CASCADE
);

-- 9. Complaints Table
CREATE TABLE IF NOT EXISTS complaints (
    id BIGSERIAL PRIMARY KEY,
    taxpayer_id BIGINT NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) DEFAULT 'OPEN',
    response TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_complaints_taxpayer FOREIGN KEY (taxpayer_id) REFERENCES taxpayers (id) ON DELETE CASCADE
);

-- 10. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    read_status BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 11. Chat History Table
CREATE TABLE IF NOT EXISTS chat_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
