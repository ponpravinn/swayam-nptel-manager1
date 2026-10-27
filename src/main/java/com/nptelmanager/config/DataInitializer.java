package com.nptelmanager.config;

import com.nptelmanager.entity.*;
import com.nptelmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Seeds demo data on startup if database is empty.
 * Creates demo admin, demo student, sample courses, assignments, and examination data.
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

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already has data. Skipping seed.");
            return;
        }

        log.info("Seeding demo data...");

        // --- Admin ---
        User admin = userRepository.save(User.builder()
            .fullName("NPTEL Admin")
            .email("admin@nptelmanager.com")
            .password(passwordEncoder.encode("Admin@123"))
            .role(User.Role.ADMIN)
            .enabled(true)
            .build());

        // --- Demo Student ---
        User student = userRepository.save(User.builder()
            .fullName("Demo Student")
            .email("student@demo.com")
            .password(passwordEncoder.encode("Student@123"))
            .role(User.Role.STUDENT)
            .college("Demo Engineering College")
            .department("Computer Science")
            .rollNumber("CS2023001")
            .enabled(true)
            .build());

        // --- Courses ---
        Course javaCourse = courseRepository.save(Course.builder()
            .courseName("Programming in Java")
            .courseCode("NPTEL-JAVA-2024")
            .instructor("Prof. Debasis Samanta")
            .duration("12 Weeks")
            .startDate(LocalDate.of(2024, 7, 22))
            .endDate(LocalDate.of(2024, 10, 11))
            .semester("Jul-Oct 2024")
            .courseUrl("https://onlinecourses.nptel.ac.in/noc24_cs62/preview")
            .isAdminCreated(true)
            .build());

        Course dsCourse = courseRepository.save(Course.builder()
            .courseName("Data Structures and Algorithms Using Python")
            .courseCode("NPTEL-DSA-2024")
            .instructor("Prof. Madhavan Mukund")
            .duration("8 Weeks")
            .startDate(LocalDate.of(2024, 7, 22))
            .endDate(LocalDate.of(2024, 9, 13))
            .semester("Jul-Oct 2024")
            .courseUrl("https://onlinecourses.nptel.ac.in/noc24_cs63/preview")
            .isAdminCreated(true)
            .build());

        // Enroll demo student
        studentCourseRepository.save(StudentCourse.builder()
            .student(student).course(javaCourse)
            .enrollmentStatus(StudentCourse.EnrollmentStatus.ENROLLED).build());
        studentCourseRepository.save(StudentCourse.builder()
            .student(student).course(dsCourse)
            .enrollmentStatus(StudentCourse.EnrollmentStatus.ENROLLED).build());

        // --- Assignments for Java Course ---
        Assignment javaA1 = assignmentRepository.save(Assignment.builder()
            .title("Week 1 Assignment - Basics of Java")
            .assignmentNumber(1)
            .course(javaCourse)
            .description("[SAMPLE DATA] Introduction to Java programming basics")
            .releaseDate(LocalDate.of(2024, 7, 22))
            .dueDate(LocalDateTime.of(2024, 7, 31, 23, 59))
            .submissionUrl("https://onlinecourses.nptel.ac.in/noc24_cs62/unit?unit=1")
            .isAdminCreated(true)
            .build());

        Assignment javaA2 = assignmentRepository.save(Assignment.builder()
            .title("Week 2 Assignment - OOP Concepts")
            .assignmentNumber(2)
            .course(javaCourse)
            .description("[SAMPLE DATA] Classes, objects, inheritance, and polymorphism")
            .releaseDate(LocalDate.of(2024, 7, 29))
            .dueDate(LocalDateTime.of(2024, 8, 7, 23, 59))
            .submissionUrl("https://onlinecourses.nptel.ac.in/noc24_cs62/unit?unit=2")
            .isAdminCreated(true)
            .build());

        Assignment javaA3 = assignmentRepository.save(Assignment.builder()
            .title("Week 3 Assignment - Collections Framework")
            .assignmentNumber(3)
            .course(javaCourse)
            .description("[SAMPLE DATA] Lists, Sets, Maps, and iterators")
            .releaseDate(LocalDate.of(2024, 8, 5))
            .dueDate(LocalDateTime.now().plusDays(5))   // upcoming
            .submissionUrl("https://onlinecourses.nptel.ac.in/noc24_cs62/unit?unit=3")
            .isAdminCreated(true)
            .build());

        // Student assignment statuses
        studentAssignmentRepository.save(StudentAssignment.builder()
            .student(student).assignment(javaA1)
            .status(StudentAssignment.AssignmentStatus.COMPLETED)
            .completedAt(LocalDateTime.of(2024, 7, 29, 15, 0))
            .marksObtained(85.0).build());

        studentAssignmentRepository.save(StudentAssignment.builder()
            .student(student).assignment(javaA2)
            .status(StudentAssignment.AssignmentStatus.COMPLETED)
            .completedAt(LocalDateTime.of(2024, 8, 5, 10, 0))
            .marksObtained(90.0).build());

        studentAssignmentRepository.save(StudentAssignment.builder()
            .student(student).assignment(javaA3)
            .status(StudentAssignment.AssignmentStatus.IN_PROGRESS).build());

        // --- DSA Assignment ---
        Assignment dsaA1 = assignmentRepository.save(Assignment.builder()
            .title("Week 1 Assignment - Arrays and Lists")
            .assignmentNumber(1)
            .course(dsCourse)
            .description("[SAMPLE DATA] Basic data structure operations")
            .releaseDate(LocalDate.of(2024, 7, 22))
            .dueDate(LocalDateTime.now().plusDays(2))  // urgent
            .isAdminCreated(true)
            .build());

        studentAssignmentRepository.save(StudentAssignment.builder()
            .student(student).assignment(dsaA1)
            .status(StudentAssignment.AssignmentStatus.NOT_STARTED).build());

        // --- Examination ---
        Examination javaExam = examinationRepository.save(Examination.builder()
            .course(javaCourse)
            .examSession("Oct 2024")
            .registrationOpenDate(LocalDate.now().minusDays(5))
            .registrationCloseDate(LocalDate.now().plusDays(10))
            .registrationFee(new BigDecimal("1000.00"))
            .registrationUrl("https://examform.nptel.ac.in")
            .examDate(LocalDate.now().plusDays(30))
            .examStartTime(LocalTime.of(9, 30))
            .examEndTime(LocalTime.of(12, 30))
            .examMode("Online Proctored / Centre Based")
            .examCenterName("[SAMPLE] Demo Exam Centre")
            .examCenterAddress("[SAMPLE] 123 Main Street, Tech Park")
            .city("Chennai")
            .importantInstructions("[SAMPLE DATA] Bring your Hall Ticket and any valid Government ID proof. Report 30 minutes before exam. No electronic devices allowed.")
            .isPublished(true)
            .build());

        examRegistrationRepository.save(ExaminationRegistration.builder()
            .student(student)
            .examination(javaExam)
            .registrationStatus(ExaminationRegistration.RegistrationStatus.REGISTRATION_OPEN)
            .paymentStatus(ExaminationRegistration.PaymentStatus.NOT_PAID)
            .build());

        log.info("✅ Demo data seeded successfully!");
        log.info("   Admin: admin@nptelmanager.com / Admin@123");
        log.info("   Student: student@demo.com / Student@123");
    }
}
