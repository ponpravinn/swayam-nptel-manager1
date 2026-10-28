package com.nptelmanager.service;

import com.nptelmanager.entity.*;
import com.nptelmanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final StudentAssignmentRepository studentAssignmentRepository;
    private final StudentCourseRepository studentCourseRepository;

    @Transactional(readOnly = true)
    public List<StudentAssignment> getStudentAssignments(User student) {
        return studentAssignmentRepository.findByStudentIdWithDetails(student.getId());
    }

    @Transactional(readOnly = true)
    public List<StudentAssignment> getStudentAssignmentsByCourse(User student, Long courseId) {
        return studentAssignmentRepository.findByStudentIdWithDetails(student.getId())
            .stream()
            .filter(sa -> sa.getAssignment().getCourse().getId().equals(courseId))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentAssignment> getByStatus(User student, StudentAssignment.AssignmentStatus status) {
        return studentAssignmentRepository.findByStudentIdAndStatus(student.getId(), status);
    }

    @Transactional(readOnly = true)
    public Optional<StudentAssignment> findStudentAssignment(User student, Long assignmentId) {
        return studentAssignmentRepository.findByStudentIdAndAssignmentId(student.getId(), assignmentId);
    }

    /**
     * Add a new assignment to a course the student is enrolled in, and create StudentAssignment record.
     */
    public StudentAssignment addAssignment(User student, Assignment assignment) {
        Assignment saved = assignmentRepository.save(assignment);
        StudentAssignment sa = StudentAssignment.builder()
            .student(student)
            .assignment(saved)
            .status(StudentAssignment.AssignmentStatus.NOT_STARTED)
            .build();
        return studentAssignmentRepository.save(sa);
    }

    /**
     * Admin adds assignment - creates StudentAssignment entries for all enrolled students.
     */
    public Assignment adminAddAssignment(Assignment assignment) {
        assignment.setAdminCreated(true);
        Assignment saved = assignmentRepository.save(assignment);
        // Create StudentAssignment records for all students enrolled in this course
        List<StudentCourse> enrolled = studentCourseRepository.findAll().stream()
            .filter(sc -> sc.getCourse().getId().equals(assignment.getCourse().getId()))
            .toList();
        for (StudentCourse sc : enrolled) {
            if (!studentAssignmentRepository.findByStudentIdAndAssignmentId(
                    sc.getStudent().getId(), saved.getId()).isPresent()) {
                studentAssignmentRepository.save(StudentAssignment.builder()
                    .student(sc.getStudent())
                    .assignment(saved)
                    .status(StudentAssignment.AssignmentStatus.NOT_STARTED)
                    .build());
            }
        }
        return saved;
    }

    public StudentAssignment markCompleted(User student, Long assignmentId, Double marks) {
        StudentAssignment sa = studentAssignmentRepository
            .findByStudentIdAndAssignmentId(student.getId(), assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment record not found"));
        sa.setStatus(StudentAssignment.AssignmentStatus.COMPLETED);
        sa.setCompletedAt(LocalDateTime.now());
        if (marks != null) sa.setMarksObtained(marks);
        return studentAssignmentRepository.save(sa);
    }

    public StudentAssignment updateStatus(User student, Long assignmentId, StudentAssignment.AssignmentStatus status) {
        StudentAssignment sa = studentAssignmentRepository
            .findByStudentIdAndAssignmentId(student.getId(), assignmentId)
            .orElseThrow(() -> new RuntimeException("Assignment record not found"));
        if (sa.getStatus() != StudentAssignment.AssignmentStatus.COMPLETED) {
            sa.setStatus(status);
            return studentAssignmentRepository.save(sa);
        }
        return sa; // don't allow changing a completed assignment
    }

    /**
     * Sync overdue status - updates all incomplete assignments past their due date.
     */
    public void syncOverdueStatuses(User student) {
        List<StudentAssignment> all = studentAssignmentRepository.findByStudentIdWithDetails(student.getId());
        for (StudentAssignment sa : all) {
            if (sa.isEffectivelyOverdue() && sa.getStatus() != StudentAssignment.AssignmentStatus.OVERDUE) {
                sa.setStatus(StudentAssignment.AssignmentStatus.OVERDUE);
                studentAssignmentRepository.save(sa);
            }
        }
    }

    public Assignment updateAssignment(Assignment assignment) {
        return assignmentRepository.save(assignment);
    }

    public void deleteAssignment(Long assignmentId) {
        assignmentRepository.deleteById(assignmentId);
    }

    @Transactional(readOnly = true)
    public long countByStatus(User student, StudentAssignment.AssignmentStatus status) {
        return studentAssignmentRepository.countByStudentIdAndStatus(student.getId(), status);
    }

    @Transactional(readOnly = true)
    public long countOverdue(User student) {
        return studentAssignmentRepository.countOverdueByStudentId(student.getId());
    }

    @Transactional(readOnly = true)
    public List<Assignment> getAllAdminAssignments() {
        return assignmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long getTotalCount() {
        return assignmentRepository.count();
    }
}
