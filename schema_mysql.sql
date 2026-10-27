-- ==========================================================
-- SWAYAM NPTEL Assignment Manager — MySQL Database Schema
-- ==========================================================

CREATE DATABASE IF NOT EXISTS nptel_manager_db;
USE nptel_manager_db;

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    college VARCHAR(200),
    department VARCHAR(100),
    roll_number VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Courses table
CREATE TABLE IF NOT EXISTS courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(255) NOT NULL,
    course_code VARCHAR(50) NOT NULL UNIQUE,
    instructor VARCHAR(150),
    duration VARCHAR(50),
    start_date DATE,
    end_date DATE,
    semester VARCHAR(50),
    description VARCHAR(500),
    course_url VARCHAR(500),
    is_admin_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Student Enrolled Courses junction table
CREATE TABLE IF NOT EXISTS student_courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sc_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sc_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT uk_student_course UNIQUE (student_id, course_id)
);

-- 4. Assignments table
CREATE TABLE IF NOT EXISTS assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    assignment_number INT NOT NULL,
    description VARCHAR(1000),
    release_date DATE,
    due_date DATETIME NOT NULL,
    submission_url VARCHAR(500),
    is_admin_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assign_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 5. Student Assignment Tracker table
CREATE TABLE IF NOT EXISTS student_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    assignment_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    completed_at DATETIME,
    marks_obtained DOUBLE,
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sa_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sa_assignment FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT uk_student_assignment UNIQUE (student_id, assignment_id)
);

-- 6. Examinations table
CREATE TABLE IF NOT EXISTS examinations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    exam_session VARCHAR(100) NOT NULL,
    registration_open_date DATE,
    registration_close_date DATE,
    registration_fee DECIMAL(10, 2),
    registration_url VARCHAR(500),
    exam_date DATE,
    exam_start_time TIME,
    exam_end_time TIME,
    exam_mode VARCHAR(50),
    exam_center_name VARCHAR(200),
    exam_center_address VARCHAR(500),
    city VARCHAR(100),
    map_url VARCHAR(500),
    important_instructions VARCHAR(2000),
    is_admin_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exam_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 7. Examination Registrations table
CREATE TABLE IF NOT EXISTS examination_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    examination_id BIGINT NOT NULL,
    registration_status VARCHAR(30) NOT NULL DEFAULT 'NOT_OPENED',
    payment_status VARCHAR(30) NOT NULL DEFAULT 'NOT_PAID',
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_er_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_er_exam FOREIGN KEY (examination_id) REFERENCES examinations(id) ON DELETE CASCADE,
    CONSTRAINT uk_student_exam UNIQUE (student_id, examination_id)
);

-- 8. Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100),
    message VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    related_assignment_id BIGINT,
    related_examination_id BIGINT,
    related_course_id BIGINT,
    deduplication_key VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_dedup (deduplication_key)
);
