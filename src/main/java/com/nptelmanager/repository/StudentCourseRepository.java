package com.nptelmanager.repository;

import com.nptelmanager.entity.StudentCourse;
import com.nptelmanager.entity.User;
import com.nptelmanager.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, Long> {

    List<StudentCourse> findByStudent(User student);
    List<StudentCourse> findByStudentId(Long studentId);
    Optional<StudentCourse> findByStudentAndCourse(User student, Course course);
    boolean existsByStudentAndCourse(User student, Course course);
    long countByCourseId(Long courseId);

    @Query("SELECT sc FROM StudentCourse sc JOIN FETCH sc.course WHERE sc.student.id = :studentId ORDER BY sc.course.courseName")
    List<StudentCourse> findByStudentIdWithCourse(@Param("studentId") Long studentId);
}
