package com.nptelmanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "examinations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Examination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String examSession;

    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate registrationOpenDate;

    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate registrationCloseDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal registrationFee;

    @Column(length = 500)
    private String registrationUrl;

    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate examDate;

    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.TIME)
    private LocalTime examStartTime;

    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.TIME)
    private LocalTime examEndTime;

    @Column(length = 50)
    private String examMode;

    @Column(length = 200)
    private String examCenterName;

    @Column(length = 500)
    private String examCenterAddress;

    @Column(length = 100)
    private String city;

    @Column(length = 500)
    private String mapUrl;

    @Column(length = 2000)
    private String importantInstructions;

    @Column(nullable = false)
    private boolean isPublished = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "examination", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.Set<ExaminationRegistration> registrations = new java.util.HashSet<>();
}
