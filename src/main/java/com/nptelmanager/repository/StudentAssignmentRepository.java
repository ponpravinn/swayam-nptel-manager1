package com.nptelmanager.repository;

import com.nptelmanager.entity.StudentAssignment;
import com.nptelmanager.entity.User;
import com.nptelmanager.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAssignmentRepository extends JpaRepository<StudentAssignment, Long> {

    Optional<StudentAssignment> findByStudentAndAssignment(User student, Assignment assignment);
    Optional<StudentAssignment> findByStudentIdAndAssignmentId(Long studentId, Long assignmentId);
    List<StudentAssignment> findByStudentId(Long studentId);

    @Query("SELECT sa FROM StudentAssignment sa JOIN FETCH sa.assignment a JOIN FETCH a.course " +
           "WHERE sa.student.id = :studentId ORDER BY a.dueDate ASC")
    List<StudentAssignment> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("SELECT sa FROM StudentAssignment sa WHERE sa.student.id = :studentId " +
           "AND sa.status = :status ORDER BY sa.assignment.dueDate ASC")
    List<StudentAssignment> findByStudentIdAndStatus(
        @Param("studentId") Long studentId,
        @Param("status") StudentAssignment.AssignmentStatus status);

    long countByStudentIdAndStatus(Long studentId, StudentAssignment.AssignmentStatus status);

    @Query("SELECT COUNT(sa) FROM StudentAssignment sa WHERE sa.student.id = :studentId " +
           "AND sa.status <> 'COMPLETED' " +
           "AND sa.assignment.dueDate < CURRENT_TIMESTAMP")
    long countOverdueByStudentId(@Param("studentId") Long studentId);
}
