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
     * Student adds (or enrolls in) a course. Creates course if not existing.
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
        return studentCourseRepository.save(sc);
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
