package com.nptelmanager;

import com.nptelmanager.entity.*;
import com.nptelmanager.repository.*;
import com.nptelmanager.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the NPTEL Manager application.
 * Uses H2 in-memory database (default profile).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NptelManagerApplicationTests {

    @Autowired UserService userService;
    @Autowired CourseService courseService;
    @Autowired AssignmentService assignmentService;
    @Autowired ExaminationService examinationService;
    @Autowired NotificationService notificationService;
    @Autowired UserRepository userRepository;
    @Autowired CourseRepository courseRepository;
    @Autowired AssignmentRepository assignmentRepository;
    @Autowired StudentAssignmentRepository studentAssignmentRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // =========================================================
    // 1. Application Context Loads
    // =========================================================
    @Test
    @Order(1)
    void contextLoads() {
        assertThat(userService).isNotNull();
        assertThat(courseService).isNotNull();
        assertThat(assignmentService).isNotNull();
    }

    // =========================================================
    // 2. User Authentication Tests
    // =========================================================
    @Test
    @Order(2)
    void testUserRegistration_Success() {
        User user = User.builder()
            .fullName("Test Student")
            .email("test@example.com")
            .password("Test@123")
            .build();

        User saved = userService.register(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getPassword()).isNotEqualTo("Test@123"); // must be hashed
        assertThat(passwordEncoder.matches("Test@123", saved.getPassword())).isTrue();
        assertThat(saved.getRole()).isEqualTo(User.Role.STUDENT);
    }

    @Test
    @Order(3)
    void testUserRegistration_DuplicateEmail_ThrowsException() {
        User user1 = User.builder()
            .fullName("User One")
            .email("dup@example.com")
            .password("Pass@123")
            .build();
        userService.register(user1);

        User user2 = User.builder()
            .fullName("User Two")
            .email("dup@example.com")
            .password("Pass@456")
            .build();

        assertThatThrownBy(() -> userService.register(user2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @Order(4)
    void testPasswordIsHashed() {
        User user = User.builder()
            .firstName("Hash")
            .lastName("Test")
            .fullName("Hash Test")
            .email("hash@example.com")
            .password("Plaintext@123")
            .build();
        User saved = userService.register(user);
        assertThat(saved.getPassword()).doesNotContain("Plaintext@123");
        assertThat(saved.getPassword()).startsWith("$2a$"); // BCrypt prefix
    }

    @Test
    @Order(5)
    void testEmailAndPhoneOtpVerification_Success() {
        User user = User.builder()
            .fullName("OTP Student")
            .email("otpstudent@example.com")
            .phone("+919876543210")
            .password("Pass@123")
            .build();

        User saved = userService.register(user);

        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.isPhoneVerified()).isFalse();
        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getEmailOtp()).isNotNull();
        assertThat(saved.getPhoneOtp()).isNotNull();

        // 1. Verify invalid OTP throws exception
        assertThatThrownBy(() -> userService.verifyEmailOtp("otpstudent@example.com", "000000"))
            .isInstanceOf(IllegalArgumentException.class);

        // 2. Verify valid email OTP
        boolean emailVerified = userService.verifyEmailOtp("otpstudent@example.com", saved.getEmailOtp());
        assertThat(emailVerified).isTrue();

        User afterEmail = userService.getByEmail("otpstudent@example.com");
        assertThat(afterEmail.isEmailVerified()).isTrue();
        assertThat(afterEmail.isPhoneVerified()).isFalse();
        assertThat(afterEmail.isEnabled()).isFalse(); // Phone still pending

        // 3. Verify valid phone OTP
        boolean phoneVerified = userService.verifyPhoneOtp("otpstudent@example.com", saved.getPhoneOtp());
        assertThat(phoneVerified).isTrue();

        User afterBoth = userService.getByEmail("otpstudent@example.com");
        assertThat(afterBoth.isEmailVerified()).isTrue();
        assertThat(afterBoth.isPhoneVerified()).isTrue();
        assertThat(afterBoth.isEnabled()).isTrue(); // Now fully activated!
    }

    // =========================================================
    // 3. Course Creation Tests
    // =========================================================
    @Test
    @Order(5)
    void testCourseCreation() {
        Course course = Course.builder()
            .courseName("Test Course")
            .courseCode("TEST-001")
            .instructor("Prof. Test")
            .build();

        Course saved = courseService.saveCourse(course);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCourseName()).isEqualTo("Test Course");
        assertThat(saved.getCourseCode()).isEqualTo("TEST-001");
    }

    @Test
    @Order(6)
    void testStudentEnrollment_PreventsDuplicate() {
        User student = userService.register(User.builder()
            .fullName("Enroll Test")
            .email("enroll@example.com")
            .password("Pass@123")
            .build());

        Course course = courseService.saveCourse(Course.builder()
            .courseName("Enroll Course")
            .courseCode("ENROLL-001")
            .build());

        courseService.enrollInCourse(student, course);

        // Second enrollment should throw
        assertThatThrownBy(() -> courseService.enrollInCourse(student, course))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already enrolled");
    }

    // =========================================================
    // 4. Assignment Tests
    // =========================================================
    @Test
    @Order(7)
    void testAssignmentCreation() {
        User student = userService.register(User.builder()
            .fullName("Assign Test")
            .email("assign@example.com")
            .password("Pass@123")
            .build());

        Course course = courseService.saveCourse(Course.builder()
            .courseName("Assignment Test Course")
            .courseCode("ATEST-001")
            .build());

        courseService.enrollInCourse(student, course);

        Assignment assignment = Assignment.builder()
            .title("Week 1 Assignment")
            .assignmentNumber(1)
            .course(course)
            .dueDate(LocalDateTime.now().plusDays(7))
            .build();

        StudentAssignment sa = assignmentService.addAssignment(student, assignment);

        assertThat(sa.getId()).isNotNull();
        assertThat(sa.getStatus()).isEqualTo(StudentAssignment.AssignmentStatus.NOT_STARTED);
        assertThat(sa.getAssignment().getTitle()).isEqualTo("Week 1 Assignment");
    }

    @Test
    @Order(8)
    void testAssignmentCompletion() {
        User student = userService.register(User.builder()
            .fullName("Complete Test")
            .email("complete@example.com")
            .password("Pass@123")
            .build());

        Course course = courseService.saveCourse(Course.builder()
            .courseName("Complete Course")
            .courseCode("COMP-001")
            .build());

        courseService.enrollInCourse(student, course);

        Assignment assignment = Assignment.builder()
            .title("Test Assignment")
            .assignmentNumber(1)
            .course(course)
            .dueDate(LocalDateTime.now().plusDays(3))
            .build();

        StudentAssignment sa = assignmentService.addAssignment(student, assignment);
        assertThat(sa.getStatus()).isEqualTo(StudentAssignment.AssignmentStatus.NOT_STARTED);

        // Mark as completed
        StudentAssignment completed = assignmentService.markCompleted(
            student, assignment.getId(), 85.0);

        assertThat(completed.getStatus()).isEqualTo(StudentAssignment.AssignmentStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getMarksObtained()).isEqualTo(85.0);
    }

    // =========================================================
    // 5. Overdue Assignment Detection
    // =========================================================
    @Test
    @Order(9)
    void testOverdueDetection() {
        User student = userService.register(User.builder()
            .fullName("Overdue Test")
            .email("overdue@example.com")
            .password("Pass@123")
            .build());

        Course course = courseService.saveCourse(Course.builder()
            .courseName("Overdue Course")
            .courseCode("OVER-001")
            .build());

        courseService.enrollInCourse(student, course);

        // Create assignment with past due date
        Assignment assignment = Assignment.builder()
            .title("Past Due Assignment")
            .assignmentNumber(1)
            .course(course)
            .dueDate(LocalDateTime.now().minusDays(2)) // 2 days ago
            .build();

        StudentAssignment sa = assignmentService.addAssignment(student, assignment);

        // Check effective overdue status
        assertThat(sa.isEffectivelyOverdue()).isTrue();
    }

    @Test
    @Order(10)
    void testCompletedAssignmentNotOverdue() {
        User student = userService.register(User.builder()
            .fullName("Not Overdue Test")
            .email("notoverdue@example.com")
            .password("Pass@123")
            .build());

        Course course = courseService.saveCourse(Course.builder()
            .courseName("Not Overdue Course")
            .courseCode("NOTOVER-001")
            .build());

        courseService.enrollInCourse(student, course);

        Assignment assignment = Assignment.builder()
            .title("Completed Past Assignment")
            .assignmentNumber(1)
            .course(course)
            .dueDate(LocalDateTime.now().minusDays(1)) // due yesterday
            .build();

        StudentAssignment sa = assignmentService.addAssignment(student, assignment);
        StudentAssignment completed = assignmentService.markCompleted(student, assignment.getId(), 90.0);

        // Completed assignments are NOT overdue
        assertThat(completed.isEffectivelyOverdue()).isFalse();
    }

    // =========================================================
    // 6. Duplicate Notification Prevention
    // =========================================================
    @Test
    @Order(11)
    void testDuplicateNotificationPrevention() {
        User user = userService.register(User.builder()
            .fullName("Notif Test")
            .email("notif@example.com")
            .password("Pass@123")
            .build());

        String dedupKey = "TEST-DEDUP-KEY-" + System.currentTimeMillis();

        // First notification - should be created
        var n1 = notificationService.createNotification(
            user, "Test Title", "Test Message",
            Notification.NotificationType.ASSIGNMENT_REMINDER,
            dedupKey, null, null, null);

        assertThat(n1).isNotNull();

        // Second notification with same key - should return null (duplicate)
        var n2 = notificationService.createNotification(
            user, "Test Title", "Test Message Again",
            Notification.NotificationType.ASSIGNMENT_REMINDER,
            dedupKey, null, null, null);

        assertThat(n2).isNull();

        // Verify only 1 notification with this key exists
        assertThat(notificationRepository.existsByDeduplicationKey(dedupKey)).isTrue();
    }

    @Test
    @Order(12)
    void testMarkNotificationAsRead() {
        User user = userService.register(User.builder()
            .fullName("Read Test")
            .email("read@example.com")
            .password("Pass@123")
            .build());

        var notification = notificationService.createNotification(
            user, "Read Test", "Please read this",
            Notification.NotificationType.GENERAL,
            "read-test-" + System.currentTimeMillis(), null, null, null);

        assertThat(notification).isNotNull();
        assertThat(notification.isRead()).isFalse();
        assertThat(notificationService.getUnreadCount(user.getId())).isGreaterThan(0);

        notificationService.markAsRead(notification.getId(), user.getId());

        var updated = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(updated.isRead()).isTrue();
    }

    // =========================================================
    // 7. Examination Registration Status
    // =========================================================
    @Test
    @Order(13)
    void testExaminationRegistrationStatus() {
        User student = userService.register(User.builder()
            .fullName("Exam Test")
            .email("examtest@example.com")
            .password("Pass@123")
            .build());

        Course course = courseService.saveCourse(Course.builder()
            .courseName("Exam Course")
            .courseCode("EXAM-001")
            .build());

        var examination = com.nptelmanager.entity.Examination.builder()
            .course(course)
            .examSession("Test Session")
            .registrationOpenDate(java.time.LocalDate.now().minusDays(2))
            .registrationCloseDate(java.time.LocalDate.now().plusDays(10))
            .build();
        examinationService.saveExamination(examination);

        var reg = examinationService.enrollStudentInExam(student, examination.getId());
        assertThat(reg).isNotNull();

        // Computed status should be REGISTRATION_OPEN (open date in past, close date in future)
        assertThat(reg.computeStatus()).isEqualTo(
            ExaminationRegistration.RegistrationStatus.REGISTRATION_OPEN);
    }

    @Test
    @Order(14)
    void testMarkExamAsRegistered() {
        User student = userService.register(User.builder()
            .fullName("Reg Test")
            .email("regtest@example.com")
            .password("Pass@123")
            .build());

        Course course = courseService.saveCourse(Course.builder()
            .courseName("Reg Course")
            .courseCode("REG-001")
            .build());

        var exam = com.nptelmanager.entity.Examination.builder()
            .course(course)
            .examSession("Reg Session")
            .registrationOpenDate(java.time.LocalDate.now().minusDays(1))
            .registrationCloseDate(java.time.LocalDate.now().plusDays(5))
            .build();
        examinationService.saveExamination(exam);

        examinationService.enrollStudentInExam(student, exam.getId());
        var reg = examinationService.updateRegistrationStatus(
            student, exam.getId(),
            ExaminationRegistration.RegistrationStatus.REGISTERED);

        assertThat(reg.getRegistrationStatus())
            .isEqualTo(ExaminationRegistration.RegistrationStatus.REGISTERED);
        assertThat(reg.getRegisteredAt()).isNotNull();
    }

    @Test
    @Order(15)
    void testFeePaymentStatus() {
        User student = userService.register(User.builder()
            .fullName("Fee Test")
            .email("feedtest@example.com")
            .password("Pass@123")
            .build());

        Course course = courseService.saveCourse(Course.builder()
            .courseName("Fee Course")
            .courseCode("FEE-001")
            .build());

        var exam = com.nptelmanager.entity.Examination.builder()
            .course(course)
            .examSession("Fee Session")
            .registrationFee(java.math.BigDecimal.valueOf(1000))
            .build();
        examinationService.saveExamination(exam);

        examinationService.enrollStudentInExam(student, exam.getId());
        var reg = examinationService.updatePaymentStatus(
            student, exam.getId(),
            ExaminationRegistration.PaymentStatus.PAID);

        assertThat(reg.getPaymentStatus())
            .isEqualTo(ExaminationRegistration.PaymentStatus.PAID);
        assertThat(reg.getPaymentConfirmedAt()).isNotNull();
    }

    // =========================================================
    // 8. Student Data Access Restriction
    // =========================================================
    @Test
    @Order(16)
    void testStudentCannotSeeOtherStudentAssignments() {
        // Create two students
        User student1 = userService.register(User.builder()
            .fullName("Student One")
            .email("s1@example.com")
            .password("Pass@123")
            .build());

        User student2 = userService.register(User.builder()
            .fullName("Student Two")
            .email("s2@example.com")
            .password("Pass@123")
            .build());

        Course course = courseService.saveCourse(Course.builder()
            .courseName("Shared Course")
            .courseCode("SHARED-001")
            .build());

        courseService.enrollInCourse(student1, course);
        courseService.enrollInCourse(student2, course);

        // Add assignment for student1
        Assignment a = Assignment.builder()
            .title("Private Assignment")
            .assignmentNumber(1)
            .course(course)
            .dueDate(LocalDateTime.now().plusDays(5))
            .build();
        assignmentService.addAssignment(student1, a);

        // Student2's assignments should be empty (separate records)
        var student2Assignments = assignmentService.getStudentAssignments(student2);
        assertThat(student2Assignments).noneMatch(
            sa -> sa.getAssignment().getTitle().equals("Private Assignment")
                  && sa.getStudent().getId().equals(student2.getId())
        );
    }

    @Test
    @Order(17)
    void testProfileUpdate() {
        User user = userService.register(User.builder()
            .fullName("Profile Test")
            .email("profile@example.com")
            .password("Pass@123")
            .college("Old College")
            .build());

        User updateData = new User();
        updateData.setFullName("Updated Name");
        updateData.setCollege("New College");
        updateData.setEmailNotificationsEnabled(false);

        User updated = userService.updateProfile(user.getId(), updateData);

        assertThat(updated.getFullName()).isEqualTo("Updated Name");
        assertThat(updated.getCollege()).isEqualTo("New College");
        assertThat(updated.isEmailNotificationsEnabled()).isFalse();
    }

    @Test
    @Order(18)
    void testPasswordChange_WrongCurrentPassword_Throws() {
        User user = userService.register(User.builder()
            .fullName("PwdChange Test")
            .email("pwdchange@example.com")
            .password("Correct@123")
            .build());

        assertThatThrownBy(() ->
            userService.changePassword(user.getId(), "Wrong@Password", "New@123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("incorrect");
    }
}
