package com.nptelmanager.repository;

import com.nptelmanager.entity.Assignment;
import com.nptelmanager.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByCourse(Course course);
    List<Assignment> findByCourseId(Long courseId);
    List<Assignment> findByCourseIdOrderByDueDateAsc(Long courseId);
    long countByCourseId(Long courseId);

    @Query("SELECT a FROM Assignment a WHERE a.course.id IN :courseIds ORDER BY a.dueDate ASC")
    List<Assignment> findByCourseIdIn(@Param("courseIds") List<Long> courseIds);

    @Query("SELECT a FROM Assignment a WHERE a.dueDate BETWEEN :start AND :end ORDER BY a.dueDate ASC")
    List<Assignment> findDueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
