package com.nptelmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_courses",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus enrollmentStatus = EnrollmentStatus.ENROLLED;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime enrolledAt;

    public enum EnrollmentStatus {
        ENROLLED, COMPLETED, DROPPED, AUDIT
    }
}
