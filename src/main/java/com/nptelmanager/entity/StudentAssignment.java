package com.nptelmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "assignment_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.NOT_STARTED;

    private LocalDateTime completedAt;

    private Double marksObtained;

    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum AssignmentStatus {
        NOT_STARTED, IN_PROGRESS, COMPLETED, OVERDUE
    }

    /**
     * Auto-determine if this assignment is overdue based on due date and current status.
     */
    public boolean isEffectivelyOverdue() {
        return status != AssignmentStatus.COMPLETED
               && assignment.getDueDate() != null
               && LocalDateTime.now().isAfter(assignment.getDueDate());
    }
}
