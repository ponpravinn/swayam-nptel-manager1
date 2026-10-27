package com.nptelmanager.repository;

import com.nptelmanager.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);
    boolean existsByCourseCode(String courseCode);

    @Query("SELECT c FROM Course c WHERE c.isAdminCreated = true ORDER BY c.courseName")
    List<Course> findAllAdminCourses();

    List<Course> findByCourseName(String courseName);
}
