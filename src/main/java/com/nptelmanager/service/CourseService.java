package com.nptelmanager.service;

import com.nptelmanager.entity.*;
import com.nptelmanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final AssignmentRepository assignmentRepository;
    private final StudentAssignmentRepository studentAssignmentRepository;
    private final ExaminationRepository examinationRepository;
    private final ExaminationRegistrationRepository examRegRepo;

    @Transactional(readOnly = true)
    public List<StudentCourse> getStudentCourses(User student) {
        return studentCourseRepository.findByStudentIdWithCourse(student.getId());
    }

    @Transactional(readOnly = true)
    public List<Course> getAllAdminCourses() {
        return courseRepository.findAllAdminCourses();
    }

    @Transactional(readOnly = true)
    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    /**
     * Student enrolls in an official course.
     * Automatically links all existing course assignments and examination details to this student!
     */
    public StudentCourse enrollInCourse(User student, Course course) {
        Course targetCourse = course;
        if (targetCourse.getId() == null) {
            if (courseRepository.existsByCourseCode(targetCourse.getCourseCode())) {
                targetCourse = courseRepository.findByCourseCode(targetCourse.getCourseCode()).orElseThrow();
            } else {
                targetCourse = courseRepository.save(targetCourse);
            }
        }
        if (studentCourseRepository.existsByStudentAndCourse(student, targetCourse)) {
            throw new IllegalArgumentException("You are already enrolled in this course.");
        }
        StudentCourse sc = StudentCourse.builder()
            .student(student)
            .course(targetCourse)
            .enrollmentStatus(StudentCourse.EnrollmentStatus.ENROLLED)
            .build();
        StudentCourse savedSc = studentCourseRepository.save(sc);

        // 1. Automatically populate all assignments for this course to the student
        List<Assignment> courseAssignments = assignmentRepository.findByCourseId(targetCourse.getId());
        for (Assignment assignment : courseAssignments) {
            if (studentAssignmentRepository.findByStudentAndAssignment(student, assignment).isEmpty()) {
                studentAssignmentRepository.save(StudentAssignment.builder()
                    .student(student)
                    .assignment(assignment)
                    .status(StudentAssignment.AssignmentStatus.NOT_STARTED)
                    .build());
            }
        }

        // 2. Automatically link official examination records for this course to the student
        List<Examination> courseExams = examinationRepository.findByCourseId(targetCourse.getId());
        for (Examination exam : courseExams) {
            if (examRegRepo.findByStudentAndExamination(student, exam).isEmpty()) {
                examRegRepo.save(ExaminationRegistration.builder()
                    .student(student)
                    .examination(exam)
                    .registrationStatus(ExaminationRegistration.RegistrationStatus.REGISTRATION_OPEN)
                    .paymentStatus(ExaminationRegistration.PaymentStatus.NOT_PAID)
                    .build());
            }
        }

        return savedSc;
    }

    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }

    public void deleteCourse(Long courseId) {
        courseRepository.deleteById(courseId);
    }

    public void unenroll(User student, Long courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        studentCourseRepository.findByStudentAndCourse(student, course)
            .ifPresent(studentCourseRepository::delete);
    }

    @Transactional(readOnly = true)
    public boolean isEnrolled(User student, Long courseId) {
        return courseRepository.findById(courseId)
            .map(c -> studentCourseRepository.existsByStudentAndCourse(student, c))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public long getAssignmentCount(Long courseId) {
        return assignmentRepository.countByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public long getEnrolledCount(Long courseId) {
        return studentCourseRepository.countByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public Optional<Examination> getExamForCourse(Long courseId) {
        List<Examination> exams = examinationRepository.findByCourseId(courseId);
        return exams.isEmpty() ? Optional.empty() : Optional.of(exams.get(0));
    }

    public static record SwayamPreset(
        String courseCode,
        String courseName,
        String instructor,
        String institute,
        String duration,
        int numberOfWeeks,
        String semester,
        String courseUrl,
        String description,
        String examSession,
        LocalDate examDate,
        LocalTime examStartTime,
        LocalTime examEndTime,
        BigDecimal examFee,
        LocalDate regOpenDate,
        LocalDate regCloseDate,
        String examCenter
    ) {}

    private static final List<SwayamPreset> OFFICIAL_PRESETS = List.of(
        new SwayamPreset(
            "noc24-cs17",
            "Cloud Computing",
            "Prof. Soumya Kanti Ghosh",
            "IIT Kharagpur",
            "8 Weeks",
            8,
            "Jul-Oct 2026",
            "https://onlinecourses.nptel.ac.in/noc24_cs17/preview",
            "Covers virtualization, cloud service models (IaaS, PaaS, SaaS), cloud storage, and security.",
            "October 2026 Shift 1",
            LocalDate.of(2026, 10, 24),
            LocalTime.of(9, 0),
            LocalTime.of(12, 0),
            new BigDecimal("1000.00"),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 8, 16),
            "TCS iON Digital Zone"
        ),
        new SwayamPreset(
            "noc24-cs03",
            "Programming in Java",
            "Prof. Debasis Samanta",
            "IIT Kharagpur",
            "12 Weeks",
            12,
            "Jul-Oct 2026",
            "https://onlinecourses.nptel.ac.in/noc24_cs03/preview",
            "Object-oriented programming, inheritance, interfaces, multithreading, and Java Collections Framework.",
            "October 2026 Shift 2",
            LocalDate.of(2026, 10, 25),
            LocalTime.of(14, 0),
            LocalTime.of(17, 0),
            new BigDecimal("1000.00"),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 8, 16),
            "TCS iON Digital Zone"
        ),
        new SwayamPreset(
            "noc24-cs39",
            "Data Science for Engineers",
            "Prof. Ragunathan Rengasamy & Prof. Shankar Narasimhan",
            "IIT Madras",
            "8 Weeks",
            8,
            "Jul-Oct 2026",
            "https://onlinecourses.nptel.ac.in/noc24_cs39/preview",
            "R programming, linear algebra, regression, classification, and statistical data visualization.",
            "October 2026 Shift 1",
            LocalDate.of(2026, 10, 25),
            LocalTime.of(9, 0),
            LocalTime.of(12, 0),
            new BigDecimal("1000.00"),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 8, 16),
            "TCS iON Digital Zone"
        ),
        new SwayamPreset(
            "noc24-cs21",
            "Database Management System",
            "Prof. Partha Pratim Das",
            "IIT Kharagpur",
            "8 Weeks",
            8,
            "Jul-Oct 2026",
            "https://onlinecourses.nptel.ac.in/noc24_cs21/preview",
            "Relational models, SQL, indexing, normalization, transactions, and concurrency control.",
            "October 2026 Shift 2",
            LocalDate.of(2026, 10, 24),
            LocalTime.of(14, 0),
            LocalTime.of(17, 0),
            new BigDecimal("1000.00"),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 8, 16),
            "TCS iON Digital Zone"
        ),
        new SwayamPreset(
            "noc24-cs48",
            "Introduction to Machine Learning",
            "Prof. Balaraman Ravindran",
            "IIT Madras",
            "12 Weeks",
            12,
            "Jul-Oct 2026",
            "https://onlinecourses.nptel.ac.in/noc24_cs48/preview",
            "Supervised learning, linear regression, SVMs, decision trees, neural networks, and clustering.",
            "November 2026 Shift 1",
            LocalDate.of(2026, 11, 1),
            LocalTime.of(9, 0),
            LocalTime.of(12, 0),
            new BigDecimal("1000.00"),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 8, 16),
            "TCS iON Digital Zone"
        ),
        new SwayamPreset(
            "noc24-cs52",
            "Deep Learning",
            "Prof. Prabir Kumar Biswas",
            "IIT Kharagpur",
            "12 Weeks",
            12,
            "Jul-Oct 2026",
            "https://onlinecourses.nptel.ac.in/noc24_cs52/preview",
            "Convolutional Neural Networks, Recurrent Networks, Autoencoders, and GAN architectures.",
            "November 2026 Shift 2",
            LocalDate.of(2026, 11, 1),
            LocalTime.of(14, 0),
            LocalTime.of(17, 0),
            new BigDecimal("1000.00"),
            LocalDate.of(2026, 6, 15),
            LocalDate.of(2026, 8, 16),
            "TCS iON Digital Zone"
        )
    );

    @Transactional(readOnly = true)
    public List<SwayamPreset> getOfficialPresets() {
        return OFFICIAL_PRESETS;
    }

    public Course publishPreset(String presetCode) {
        SwayamPreset preset = OFFICIAL_PRESETS.stream()
            .filter(p -> p.courseCode.equalsIgnoreCase(presetCode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown course preset: " + presetCode));

        if (courseRepository.existsByCourseCode(preset.courseCode)) {
            return courseRepository.findByCourseCode(preset.courseCode).orElseThrow();
        }

        Course course = Course.builder()
            .courseName(preset.courseName)
            .courseCode(preset.courseCode)
            .instructor(preset.instructor + " (" + preset.institute + ")")
            .duration(preset.duration)
            .semester(preset.semester)
            .courseUrl(preset.courseUrl)
            .description(preset.description)
            .startDate(LocalDate.now().minusWeeks(2))
            .endDate(LocalDate.now().plusWeeks(preset.numberOfWeeks))
            .isAdminCreated(true)
            .build();
        Course savedCourse = courseRepository.save(course);

        // Auto-create weekly assignments
        LocalDateTime baseDue = LocalDateTime.now().plusDays(6).withHour(23).withMinute(59).withSecond(0);
        for (int w = 1; w <= preset.numberOfWeeks; w++) {
            LocalDateTime due = baseDue.plusWeeks(w - 1);
            Assignment assignment = Assignment.builder()
                .course(savedCourse)
                .title("Week " + w + " - " + preset.courseName + " Graded Assignment")
                .assignmentNumber(w)
                .releaseDate(due.minusDays(7).toLocalDate())
                .dueDate(due)
                .description("Official SWAYAM NPTEL weekly assignment for Week " + w)
                .submissionUrl(preset.courseUrl)
                .isAdminCreated(true)
                .build();
            assignmentRepository.save(assignment);
        }

        // Auto-create official examination schedule
        Examination exam = Examination.builder()
            .course(savedCourse)
            .examSession(preset.examSession)
            .examDate(preset.examDate)
            .examStartTime(preset.examStartTime)
            .examEndTime(preset.examEndTime)
            .registrationFee(preset.examFee)
            .registrationOpenDate(preset.regOpenDate)
            .registrationCloseDate(preset.regCloseDate)
            .examMode("Computer Based Test (CBT) at Designated Center")
            .examCenterName(preset.examCenter)
            .registrationUrl("https://examform.nptel.ac.in")
            .isPublished(true)
            .build();
        examinationRepository.save(exam);

        return savedCourse;
    }

    public Course createFullCourse(
            Course course,
            boolean generateAssignments,
            Integer numberOfWeeks,
            LocalDateTime firstAssignmentDueDate,
            boolean scheduleExam,
            String examSession,
            LocalDate examDate,
            LocalTime examStartTime,
            LocalTime examEndTime,
            BigDecimal registrationFee,
            LocalDate registrationOpenDate,
            LocalDate registrationCloseDate,
            String examCenter,
            String examMode) {

        course.setAdminCreated(true);
        Course savedCourse = courseRepository.save(course);

        if (generateAssignments && numberOfWeeks != null && numberOfWeeks > 0) {
            LocalDateTime baseDueDate = firstAssignmentDueDate != null
                ? firstAssignmentDueDate
                : LocalDateTime.now().plusDays(7).withHour(23).withMinute(59).withSecond(0);
            for (int w = 1; w <= numberOfWeeks; w++) {
                LocalDateTime dueDate = baseDueDate.plusWeeks(w - 1);
                Assignment assignment = Assignment.builder()
                    .course(savedCourse)
                    .title("Week " + w + " Assignment - " + savedCourse.getCourseName())
                    .assignmentNumber(w)
                    .releaseDate(dueDate.minusDays(7).toLocalDate())
                    .dueDate(dueDate)
                    .description("Weekly graded assignment for Week " + w)
                    .submissionUrl(savedCourse.getCourseUrl() != null && !savedCourse.getCourseUrl().isBlank()
                        ? savedCourse.getCourseUrl()
                        : "https://onlinecourses.nptel.ac.in")
                    .isAdminCreated(true)
                    .build();
                assignmentRepository.save(assignment);
            }
        }

        if (scheduleExam) {
            Examination exam = Examination.builder()
                .course(savedCourse)
                .examSession(examSession != null && !examSession.isBlank() ? examSession : "NPTEL In-Person Examination")
                .examDate(examDate != null ? examDate : LocalDate.now().plusWeeks(12))
                .examStartTime(examStartTime != null ? examStartTime : LocalTime.of(9, 0))
                .examEndTime(examEndTime != null ? examEndTime : LocalTime.of(12, 0))
                .registrationFee(registrationFee != null ? registrationFee : new BigDecimal("1000.00"))
                .registrationOpenDate(registrationOpenDate != null ? registrationOpenDate : LocalDate.now().plusWeeks(2))
                .registrationCloseDate(registrationCloseDate != null ? registrationCloseDate : LocalDate.now().plusWeeks(8))
                .examMode(examMode != null && !examMode.isBlank() ? examMode : "Computer Based Test (CBT)")
                .examCenterName(examCenter != null && !examCenter.isBlank() ? examCenter : "TCS iON Digital Zone")
                .registrationUrl("https://examform.nptel.ac.in")
                .isPublished(true)
                .build();
            examinationRepository.save(exam);
        }

        return savedCourse;
    }
}
