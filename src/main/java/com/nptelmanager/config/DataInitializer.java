package com.nptelmanager.config;

import com.nptelmanager.entity.*;
import com.nptelmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Seeds authentic SWAYAM NPTEL courses and assignments into the database.
 * Reflects real student coursework:
 * 1. The Joy of Computing using Python (1st Year - Completed)
 * 2. Introduction to Internet of Things (IoT) (Current Semester - Active)
 * 3. Organizational Behaviour (Current Semester - Active)
 * 4. Programming in Java & Cloud Computing (Available in Catalog)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentAssignmentRepository studentAssignmentRepository;
    private final ExaminationRepository examinationRepository;
    private final ExaminationRegistrationRepository examRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Checking and seeding authentic SWAYAM NPTEL course data...");

        // Auto-migrate column sizes for existing database files (both H2 and MySQL supported)
        migrateColumnLengths();

        // 1. Ensure Admin and Student accounts exist
        User admin = userRepository.findByEmail("admin@nptelmanager.com")
            .orElseGet(() -> userRepository.save(User.builder()
                .fullName("NPTEL Admin / SPOC")
                .email("admin@nptelmanager.com")
                .password(passwordEncoder.encode("Admin@123"))
                .role(User.Role.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .phoneVerified(true)
                .build()));
        if (!admin.isEmailVerified()) {
            admin.setEmailVerified(true);
            admin.setPhoneVerified(true);
            userRepository.save(admin);
        }

        User student = userRepository.findByEmail("student@demo.com")
            .orElseGet(() -> userRepository.save(User.builder()
                .fullName("Demo Student")
                .email("student@demo.com")
                .password(passwordEncoder.encode("Student@123"))
                .role(User.Role.STUDENT)
                .phone("+91 9876543210")
                .college("Engineering College")
                .department("Computer Science and Engineering")
                .rollNumber("CSE202401")
                .enabled(true)
                .emailVerified(true)
                .phoneVerified(true)
                .whatsappNotificationsEnabled(true)
                .build()));

        if (student.getPhone() == null || student.getPhone().isBlank() || !student.isEmailVerified()) {
            student.setPhone("+91 9876543210");
            student.setEmailVerified(true);
            student.setPhoneVerified(true);
            student.setWhatsappNotificationsEnabled(true);
            userRepository.save(student);
        }

        // 2. Course: The Joy of Computing using Python (1st Year - Completed)
        if (!courseRepository.existsByCourseCode("noc23-cs108")) {
            Course joyPython = courseRepository.save(Course.builder()
                .courseName("The Joy of Computing using Python")
                .courseCode("noc23-cs108")
                .instructor("Prof. Sudarshan Iyengar")
                .duration("12 Weeks")
                .startDate(LocalDate.of(2023, 1, 23))
                .endDate(LocalDate.of(2023, 4, 14))
                .semester("Jan-Apr 2023 (1st Year)")
                .courseUrl("https://onlinecourses.nptel.ac.in/noc23_cs108/preview")
                .isAdminCreated(true)
                .build());

            studentCourseRepository.save(StudentCourse.builder()
                .student(student).course(joyPython)
                .enrollmentStatus(StudentCourse.EnrollmentStatus.COMPLETED)
                .build());

            Assignment a1 = createAssignment(joyPython, 1, "Week 1 - Scratch & Python Basics",
                "Introduction to variables, loops, and turtle graphics in Python.",
                LocalDate.of(2023, 1, 23), LocalDateTime.of(2023, 2, 1, 23, 59),
                "https://onlinecourses.nptel.ac.in/noc23_cs108/unit?unit=1");
            createStudentAssignment(student, a1, StudentAssignment.AssignmentStatus.COMPLETED, 95.0, LocalDateTime.of(2023, 1, 30, 18, 30));

            Assignment a2 = createAssignment(joyPython, 2, "Week 2 - Lists, Tuples & Rock Paper Scissors",
                "Working with collection data structures and building interactive games.",
                LocalDate.of(2023, 1, 30), LocalDateTime.of(2023, 2, 8, 23, 59),
                "https://onlinecourses.nptel.ac.in/noc23_cs108/unit?unit=2");
            createStudentAssignment(student, a2, StudentAssignment.AssignmentStatus.COMPLETED, 90.0, LocalDateTime.of(2023, 2, 6, 20, 15));

            Assignment a3 = createAssignment(joyPython, 3, "Week 3 - Functions & The Monty Hall Simulation",
                "Modular programming, random simulations, and probability paradoxes in Python.",
                LocalDate.of(2023, 2, 6), LocalDateTime.of(2023, 2, 15, 23, 59),
                "https://onlinecourses.nptel.ac.in/noc23_cs108/unit?unit=3");
            createStudentAssignment(student, a3, StudentAssignment.AssignmentStatus.COMPLETED, 92.0, LocalDateTime.of(2023, 2, 14, 21, 0));

            createExam(joyPython, "Apr 2023 Session",
                LocalDate.of(2023, 2, 1), LocalDate.of(2023, 2, 20),
                new BigDecimal("1000.00"), "https://examform.nptel.ac.in",
                LocalDate.of(2023, 4, 29), LocalTime.of(9, 30), LocalTime.of(12, 30),
                "Online Proctored / Center Based", "TCS iON Digital Zone",
                "Sector 62, Tech City", "Chennai",
                "Hall ticket and government ID mandatory. Certification issued by IIT Ropar.",
                student, ExaminationRegistration.RegistrationStatus.REGISTERED,
                ExaminationRegistration.PaymentStatus.PAID);
        }

        // 3. Course: Introduction to Internet of Things (IoT) (Current Semester - Active)
        if (!courseRepository.existsByCourseCode("noc24-cs102")) {
            Course iotCourse = courseRepository.save(Course.builder()
                .courseName("Introduction to Internet of Things")
                .courseCode("noc24-cs102")
                .instructor("Prof. Sudip Misra (IIT Kharagpur)")
                .duration("12 Weeks")
                .startDate(LocalDate.now().minusWeeks(4))
                .endDate(LocalDate.now().plusWeeks(8))
                .semester("Current Semester")
                .courseUrl("https://onlinecourses.nptel.ac.in/noc24_cs102/preview")
                .isAdminCreated(true)
                .build());

            studentCourseRepository.save(StudentCourse.builder()
                .student(student).course(iotCourse)
                .enrollmentStatus(StudentCourse.EnrollmentStatus.ENROLLED)
                .build());

            Assignment a1 = createAssignment(iotCourse, 1, "Week 1 Assignment - IoT Architecture & Sensing",
                "Sensors, actuators, and foundational IoT reference architecture.",
                LocalDate.now().minusWeeks(3), LocalDateTime.now().minusDays(10),
                "https://onlinecourses.nptel.ac.in/noc24_cs102/unit?unit=1");
            createStudentAssignment(student, a1, StudentAssignment.AssignmentStatus.COMPLETED, 88.0, LocalDateTime.now().minusDays(11));

            Assignment a2 = createAssignment(iotCourse, 2, "Week 2 Assignment - Wireless Communication Protocols",
                "Analysis of BLE, Zigbee, LoRaWAN, and IPv6 over Low-Power WPAN.",
                LocalDate.now().minusWeeks(2), LocalDateTime.now().minusDays(3),
                "https://onlinecourses.nptel.ac.in/noc24_cs102/unit?unit=2");
            createStudentAssignment(student, a2, StudentAssignment.AssignmentStatus.COMPLETED, 94.0, LocalDateTime.now().minusDays(4));

            Assignment a3 = createAssignment(iotCourse, 3, "Week 3 Assignment - Arduino & Raspberry Pi Interfacing",
                "Embedded hardware interfacing, GPIO programming, and serial sensor communication.",
                LocalDate.now().minusWeeks(1), LocalDateTime.now().plusDays(3), // DUE IN 3 DAYS!
                "https://onlinecourses.nptel.ac.in/noc24_cs102/unit?unit=3");
            createStudentAssignment(student, a3, StudentAssignment.AssignmentStatus.IN_PROGRESS, null, null);

            Assignment a4 = createAssignment(iotCourse, 4, "Week 4 Assignment - MQTT Protocol & Cloud IoT",
                "Publish/Subscribe messaging architectures, broker setups, and cloud analytics telemetry.",
                LocalDate.now(), LocalDateTime.now().plusDays(7), // DUE IN 7 DAYS!
                "https://onlinecourses.nptel.ac.in/noc24_cs102/unit?unit=4");
            createStudentAssignment(student, a4, StudentAssignment.AssignmentStatus.NOT_STARTED, null, null);

            createExam(iotCourse, "Upcoming Examination Session",
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(6), // Registration closes in 6 days!
                new BigDecimal("1000.00"), "https://examform.nptel.ac.in",
                LocalDate.now().plusDays(28), LocalTime.of(9, 30), LocalTime.of(12, 30),
                "Center Based Computer Exam", "TCS iON Digital Zone",
                "Plot No 14, Guindy Industrial Estate", "Chennai",
                "Report at test center 45 minutes prior. Original college/Aadhaar ID required. Mobile phones strictly prohibited.",
                student, ExaminationRegistration.RegistrationStatus.REGISTRATION_OPEN,
                ExaminationRegistration.PaymentStatus.NOT_PAID);
        }

        // 4. Course: Organizational Behaviour (Current Semester - Active)
        if (!courseRepository.existsByCourseCode("noc24-mg45")) {
            Course obCourse = courseRepository.save(Course.builder()
                .courseName("Organizational Behaviour")
                .courseCode("noc24-mg45")
                .instructor("Prof. Susmita Mukhopadhyay (IIT Kharagpur)")
                .duration("8 Weeks")
                .startDate(LocalDate.now().minusWeeks(3))
                .endDate(LocalDate.now().plusWeeks(5))
                .semester("Current Semester")
                .courseUrl("https://onlinecourses.nptel.ac.in/noc24_mg45/preview")
                .isAdminCreated(true)
                .build());

            studentCourseRepository.save(StudentCourse.builder()
                .student(student).course(obCourse)
                .enrollmentStatus(StudentCourse.EnrollmentStatus.ENROLLED)
                .build());

            Assignment a1 = createAssignment(obCourse, 1, "Week 1 - Personality, Perception and Attitude",
                "Core behavioral dynamics, Big Five personality traits, and attribution theory.",
                LocalDate.now().minusWeeks(2), LocalDateTime.now().minusDays(5),
                "https://onlinecourses.nptel.ac.in/noc24_mg45/unit?unit=1");
            createStudentAssignment(student, a1, StudentAssignment.AssignmentStatus.COMPLETED, 90.0, LocalDateTime.now().minusDays(6));

            Assignment a2 = createAssignment(obCourse, 2, "Week 2 - Motivation Theories & Workplace Dynamics",
                "Maslow hierarchy, Herzberg dual-factor theory, and equity in modern organizations.",
                LocalDate.now().minusWeeks(1), LocalDateTime.now().plusDays(4), // DUE IN 4 DAYS!
                "https://onlinecourses.nptel.ac.in/noc24_mg45/unit?unit=2");
            createStudentAssignment(student, a2, StudentAssignment.AssignmentStatus.IN_PROGRESS, null, null);

            Assignment a3 = createAssignment(obCourse, 3, "Week 3 - Leadership, Conflict Management & Team Culture",
                "Transformational leadership, team cohesion, and negotiation strategies in technical teams.",
                LocalDate.now(), LocalDateTime.now().plusDays(10),
                "https://onlinecourses.nptel.ac.in/noc24_mg45/unit?unit=3");
            createStudentAssignment(student, a3, StudentAssignment.AssignmentStatus.NOT_STARTED, null, null);

            createExam(obCourse, "Upcoming Examination Session",
                LocalDate.now().minusDays(8), LocalDate.now().plusDays(12),
                new BigDecimal("1000.00"), "https://examform.nptel.ac.in",
                LocalDate.now().plusDays(29), LocalTime.of(14, 0), LocalTime.of(17, 0),
                "Center Based Computer Exam", "TCS iON Digital Zone",
                "Guindy Tech Park", "Chennai",
                "Afternoon Shift (2:00 PM - 5:00 PM). Carry printed admit card and government ID proof.",
                student, ExaminationRegistration.RegistrationStatus.REGISTRATION_OPEN,
                ExaminationRegistration.PaymentStatus.PAID);
        }

        // 5. Course: Cloud Computing (Published in Catalog for Enrollment)
        if (!courseRepository.existsByCourseCode("noc24-cs78")) {
            Course cloudCourse = courseRepository.save(Course.builder()
                .courseName("Cloud Computing")
                .courseCode("noc24-cs78")
                .instructor("Prof. Soumya Kanti Ghosh (IIT Kharagpur)")
                .duration("8 Weeks")
                .startDate(LocalDate.now().minusWeeks(2))
                .endDate(LocalDate.now().plusWeeks(6))
                .semester("Current Semester")
                .courseUrl("https://onlinecourses.nptel.ac.in/noc24_cs78/preview")
                .isAdminCreated(true)
                .build());

            createAssignment(cloudCourse, 1, "Week 1 - Virtualization and Hypervisors",
                "Hardware virtualization, type-1 vs type-2 hypervisors, and containerization fundamentals.",
                LocalDate.now().minusWeeks(1), LocalDateTime.now().plusDays(5),
                "https://onlinecourses.nptel.ac.in/noc24_cs78/unit?unit=1");

            createAssignment(cloudCourse, 2, "Week 2 - Cloud Storage Architectures & AWS S3",
                "Block, object, and file storage models, consistency, and replication policies.",
                LocalDate.now(), LocalDateTime.now().plusDays(11),
                "https://onlinecourses.nptel.ac.in/noc24_cs78/unit?unit=2");

            createExam(cloudCourse, "Upcoming Examination Session",
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(15),
                new BigDecimal("1000.00"), "https://examform.nptel.ac.in",
                LocalDate.now().plusDays(35), LocalTime.of(9, 30), LocalTime.of(12, 30),
                "Center Based Computer Exam", "TCS iON Digital Zone",
                "Guindy Tech Park", "Chennai",
                "Morning shift examination. Certified by IIT Kharagpur.",
                null, null, null);
        }

        log.info("✅ Authentic SWAYAM NPTEL courses successfully seeded!");
    }

    private Assignment createAssignment(Course course, int number, String title, String desc,
                                        LocalDate release, LocalDateTime due, String url) {
        return assignmentRepository.save(Assignment.builder()
            .course(course)
            .assignmentNumber(number)
            .title(title)
            .description(desc)
            .releaseDate(release)
            .dueDate(due)
            .submissionUrl(url)
            .isAdminCreated(true)
            .build());
    }

    private void createStudentAssignment(User student, Assignment assignment,
                                         StudentAssignment.AssignmentStatus status,
                                         Double marks, LocalDateTime completedAt) {
        studentAssignmentRepository.save(StudentAssignment.builder()
            .student(student)
            .assignment(assignment)
            .status(status)
            .marksObtained(marks)
            .completedAt(completedAt)
            .build());
    }

    private void createExam(Course course, String session, LocalDate openDate, LocalDate closeDate,
                            BigDecimal fee, String url, LocalDate examDate, LocalTime start, LocalTime end,
                            String mode, String centerName, String centerAddress, String city, String instructions,
                            User student, ExaminationRegistration.RegistrationStatus regStatus,
                            ExaminationRegistration.PaymentStatus payStatus) {
        Examination exam = examinationRepository.save(Examination.builder()
            .course(course)
            .examSession(session)
            .registrationOpenDate(openDate)
            .registrationCloseDate(closeDate)
            .registrationFee(fee)
            .registrationUrl(url)
            .examDate(examDate)
            .examStartTime(start)
            .examEndTime(end)
            .examMode(mode)
            .examCenterName(centerName)
            .examCenterAddress(centerAddress)
            .city(city)
            .importantInstructions(instructions)
            .isPublished(true)
            .build());

        if (student != null && regStatus != null) {
            examRegistrationRepository.save(ExaminationRegistration.builder()
                .student(student)
                .examination(exam)
                .registrationStatus(regStatus)
                .paymentStatus(payStatus)
                .build());
        }
    }

    private void migrateColumnLengths() {
        String[] h2AlterStatements = {
            "ALTER TABLE users ALTER COLUMN roll_number VARCHAR(50)",
            "ALTER TABLE users ALTER COLUMN phone VARCHAR(20)",
            "ALTER TABLE users ALTER COLUMN college VARCHAR(150)",
            "ALTER TABLE users ALTER COLUMN department VARCHAR(100)",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS whatsapp_notifications_enabled BOOLEAN DEFAULT TRUE",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT TRUE",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT TRUE",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_otp VARCHAR(10)",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_otp_expiry TIMESTAMP",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_otp VARCHAR(10)",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_otp_expiry TIMESTAMP",
            "ALTER TABLE notifications ADD COLUMN IF NOT EXISTS sent_via_whatsapp BOOLEAN DEFAULT FALSE",
            "ALTER TABLE notifications ADD COLUMN IF NOT EXISTS whatsapp_status VARCHAR(50)",
            "UPDATE users SET email_verified = TRUE WHERE email_verified IS NULL",
            "UPDATE users SET phone_verified = TRUE WHERE phone_verified IS NULL"
        };
        for (String sql : h2AlterStatements) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception ignored) {
            }
        }
        String[] mysqlAlterStatements = {
            "ALTER TABLE users MODIFY COLUMN roll_number VARCHAR(50)",
            "ALTER TABLE users MODIFY COLUMN phone VARCHAR(20)",
            "ALTER TABLE users MODIFY COLUMN college VARCHAR(150)",
            "ALTER TABLE users MODIFY COLUMN department VARCHAR(100)",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT TRUE",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT TRUE",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_otp VARCHAR(10)",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS email_otp_expiry DATETIME",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_otp VARCHAR(10)",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_otp_expiry DATETIME"
        };
        for (String sql : mysqlAlterStatements) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception ignored) {
            }
        }
    }
}
