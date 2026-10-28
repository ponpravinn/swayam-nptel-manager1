package com.nptelmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_dedup", columnList = "deduplicationKey")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private boolean isRead = false;

    // Link back to the related entity (optional)
    private Long relatedAssignmentId;
    private Long relatedExaminationId;
    private Long relatedCourseId;

    // Deduplication key: type + entity ID + reminder period label
    @Column(length = 200, unique = true)
    private String deduplicationKey;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        ASSIGNMENT_REMINDER,
        ASSIGNMENT_OVERDUE,
        EXAM_REGISTRATION_OPENING,
        EXAM_REGISTRATION_DEADLINE,
        EXAM_REGISTRATION_MISSED,
        EXAM_FEE_PENDING,
        EXAM_DATE_REMINDER,
        GENERAL,
        SYSTEM
    }
}
