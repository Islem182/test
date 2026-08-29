-- DGI Online Services Platform - Database Schema (PostgreSQL 16)

-- Drop tables if exists in reverse order of foreign key dependencies
DROP TABLE IF EXISTS chat_history CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS complaints CASCADE;
DROP TABLE IF EXISTS fiscal_numbers CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS activities CASCADE;
DROP TABLE IF EXISTS tax_applications CASCADE;
DROP TABLE IF EXISTS taxpayers CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Roles Table
CREATE TABLE roles (
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
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- 4. Taxpayers Table
CREATE TABLE taxpayers (
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
CREATE TABLE tax_applications (
    id BIGSERIAL PRIMARY KEY,
    taxpayer_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_applications_taxpayer FOREIGN KEY (taxpayer_id) REFERENCES taxpayers (id) ON DELETE CASCADE
);

-- 6. Activities Table
CREATE TABLE activities (
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
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    upload_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    verification_status VARCHAR(30) DEFAULT 'PENDING',
    CONSTRAINT fk_documents_application FOREIGN KEY (application_id) REFERENCES tax_applications (id) ON DELETE CASCADE
);

-- 8. Fiscal Numbers Table
CREATE TABLE fiscal_numbers (
    id BIGSERIAL PRIMARY KEY,
    taxpayer_id BIGINT NOT NULL UNIQUE,
    fiscal_number VARCHAR(50) NOT NULL UNIQUE,
    generated_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fiscal_numbers_taxpayer FOREIGN KEY (taxpayer_id) REFERENCES taxpayers (id) ON DELETE CASCADE
);

-- 9. Complaints Table
CREATE TABLE complaints (
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
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    read_status BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 11. Chat History Table
CREATE TABLE chat_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Indexes for optimal query performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_taxpayers_cin ON taxpayers(cin);
CREATE INDEX idx_tax_applications_taxpayer ON tax_applications(taxpayer_id);
CREATE INDEX idx_complaints_taxpayer ON complaints(taxpayer_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);
