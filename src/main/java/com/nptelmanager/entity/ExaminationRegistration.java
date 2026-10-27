package com.nptelmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "examination_registrations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "examination_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExaminationRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus registrationStatus = RegistrationStatus.NOT_OPENED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.NOT_PAID;

    private LocalDateTime registeredAt;

    private LocalDateTime paymentConfirmedAt;

    private String transactionId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum RegistrationStatus {
        NOT_OPENED, REGISTRATION_OPEN, REGISTERED, REGISTRATION_CLOSED
    }

    public enum PaymentStatus {
        NOT_PAID, PAID, PAYMENT_PENDING
    }

    /**
     * Auto-compute registration status based on dates if student hasn't registered yet.
     */
    public RegistrationStatus computeStatus() {
        if (registrationStatus == RegistrationStatus.REGISTERED) {
            return RegistrationStatus.REGISTERED;
        }
        var today = java.time.LocalDate.now();
        if (examination.getRegistrationOpenDate() == null) {
            return RegistrationStatus.NOT_OPENED;
        }
        if (today.isBefore(examination.getRegistrationOpenDate())) {
            return RegistrationStatus.NOT_OPENED;
        }
        if (examination.getRegistrationCloseDate() != null && today.isAfter(examination.getRegistrationCloseDate())) {
            return RegistrationStatus.REGISTRATION_CLOSED;
        }
        return RegistrationStatus.REGISTRATION_OPEN;
    }
}
