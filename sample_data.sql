-- ==========================================================
-- SWAYAM NPTEL Assignment Manager — Sample Seed Data (MySQL)
-- ==========================================================

USE nptel_manager_db;

-- 1. Insert Default Users (Pass: Student@123 / Admin@123)
-- BCrypt hash for "Student@123": $2a$10$e8wF0iB8oOqKqE7b1h9wUu5X9eU5eU5eU5eU5eU5eU5eU5eU5eU5e
-- BCrypt hash for "Admin@123":   $2a$10$e8wF0iB8oOqKqE7b1h9wUu5X9eU5eU5eU5eU5eU5eU5eU5eU5eU5e
INSERT INTO users (id, full_name, email, password, phone, college, department, roll_number, role, enabled, email_notifications_enabled)
VALUES
(1, 'Demo Student', 'student@demo.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '9876543210', 'Demo Engineering College', 'Computer Science', 'CS2024-001', 'STUDENT', TRUE, TRUE),
(2, 'System Administrator', 'admin@nptelmanager.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '9876543211', 'NPTEL Center', 'Administration', 'ADMIN-001', 'ADMIN', TRUE, TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- 2. Insert Official Courses
INSERT INTO courses (id, course_name, course_code, instructor, duration, start_date, end_date, semester, description, course_url, is_admin_published)
VALUES
(1, 'Programming in Java', 'noc24-cs101', 'Prof. Debasis Samanta (IIT Kharagpur)', '12 Weeks', '2024-07-22', '2024-10-18', 'Jul-Oct 2024', 'Covers core Java, OOP, Multithreading, Generics, and Collections.', 'https://onlinecourses.nptel.ac.in/noc24_cs101/preview', TRUE),
(2, 'Deep Learning', 'noc24-cs102', 'Prof. Prabir Kumar Biswas (IIT Kharagpur)', '12 Weeks', '2024-07-22', '2024-10-18', 'Jul-Oct 2024', 'Foundations of Deep Learning, CNNs, RNNs, and Transformers.', 'https://onlinecourses.nptel.ac.in/noc24_cs102/preview', TRUE),
(3, 'Cloud Computing', 'noc24-cs103', 'Prof. Soumya Kanti Ghosh (IIT Kharagpur)', '8 Weeks', '2024-08-19', '2024-10-11', 'Jul-Oct 2024', 'Cloud architectures, virtualization, resource management, and AWS/GCP.', 'https://onlinecourses.nptel.ac.in/noc24_cs103/preview', TRUE)
ON DUPLICATE KEY UPDATE course_name = VALUES(course_name);

-- 3. Enroll Demo Student
INSERT INTO student_courses (student_id, course_id, enrollment_status)
VALUES
(1, 1, 'ENROLLED'),
(1, 2, 'ENROLLED')
ON DUPLICATE KEY UPDATE enrollment_status = 'ENROLLED';

-- 4. Insert Sample Assignments
INSERT INTO assignments (id, course_id, title, assignment_number, description, release_date, due_date, submission_url, is_admin_published)
VALUES
(1, 1, 'Week 1 Assignment - Basics & Data Types', 1, 'Variables, operators, conditional statements.', '2024-07-22', '2024-07-31 23:59:00', 'https://onlinecourses.nptel.ac.in', TRUE),
(2, 1, 'Week 2 Assignment - Classes and Objects', 2, 'Constructors, methods, inheritance, and polymorphism.', '2024-07-29', '2024-08-07 23:59:00', 'https://onlinecourses.nptel.ac.in', TRUE),
(3, 1, 'Week 3 Assignment - Packages and Interfaces', 3, 'Interface design and access protection.', '2024-08-05', '2026-12-31 23:59:00', 'https://onlinecourses.nptel.ac.in', TRUE),
(4, 2, 'Week 1 Assignment - Linear Algebra & Optimization', 1, 'Vector calculus, gradients, gradient descent.', '2024-07-22', '2024-07-31 23:59:00', 'https://onlinecourses.nptel.ac.in', TRUE),
(5, 2, 'Week 2 Assignment - Multi-layer Perceptrons', 2, 'Forward/backpropagation and activation functions.', '2024-07-29', '2026-12-31 23:59:00', 'https://onlinecourses.nptel.ac.in', TRUE)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- 5. Student Assignment Progress
INSERT INTO student_assignments (student_id, assignment_id, status, completed_at, marks_obtained)
VALUES
(1, 1, 'COMPLETED', '2024-07-30 18:30:00', 95.0),
(1, 2, 'COMPLETED', '2024-08-06 20:15:00', 90.0),
(1, 3, 'IN_PROGRESS', NULL, NULL),
(1, 4, 'COMPLETED', '2024-07-30 22:00:00', 88.0),
(1, 5, 'NOT_STARTED', NULL, NULL)
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- 6. Insert Examination Schedules
INSERT INTO examinations (id, course_id, exam_session, registration_open_date, registration_close_date, registration_fee, registration_url, exam_date, exam_start_time, exam_end_time, exam_mode, exam_center_name, exam_center_address, city, map_url, important_instructions, is_admin_published)
VALUES
(1, 1, 'Oct 2024 - Morning Session', '2024-08-01', '2026-12-15', 1000.00, 'https://examform.nptel.ac.in', '2026-10-27', '09:00:00', '12:00:00', 'Computer-based (CBT)', 'Ion Digital Zone iDZ 1', 'Sector 62, Electronic City', 'Noida', 'https://maps.google.com/?q=Ion+Digital+Zone+Noida', 'Bring printed hall ticket and valid college ID card.', TRUE),
(2, 2, 'Oct 2024 - Afternoon Session', '2024-08-01', '2026-12-15', 1000.00, 'https://examform.nptel.ac.in', '2026-10-27', '14:00:00', '17:00:00', 'Computer-based (CBT)', 'Ion Digital Zone iDZ 2', 'Sector 62, Electronic City', 'Noida', 'https://maps.google.com/?q=Ion+Digital+Zone+Noida', 'Scientific calculator is permitted. Bring original Photo ID.', TRUE)
ON DUPLICATE KEY UPDATE exam_session = VALUES(exam_session);

-- 7. Student Exam Registration Trackers
INSERT INTO examination_registrations (student_id, examination_id, registration_status, payment_status)
VALUES
(1, 1, 'REGISTRATION_OPEN', 'PAYMENT_PENDING'),
(1, 2, 'REGISTRATION_OPEN', 'NOT_PAID')
ON DUPLICATE KEY UPDATE registration_status = VALUES(registration_status);
