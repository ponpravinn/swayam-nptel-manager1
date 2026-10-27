package com.nptelmanager.service;

import com.nptelmanager.entity.*;
import com.nptelmanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
