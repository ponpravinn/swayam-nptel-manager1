package com.nptelmanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(nullable = false, length = 100)
    private String fullName;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Column(length = 20)
    private String phone;

    @Size(max = 150, message = "College name cannot exceed 150 characters")
    @Column(length = 150)
    private String college;

    @Size(max = 100, message = "Department name cannot exceed 100 characters")
    @Column(length = 100)
    private String department;

    @Size(max = 50, message = "Roll number cannot exceed 50 characters")
    @Column(length = 50)
    private String rollNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean emailNotificationsEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean whatsappNotificationsEnabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean emailVerified = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean phoneVerified = false;

    @Column(length = 10)
    private String emailOtp;

    private LocalDateTime emailOtpExpiry;

    @Column(length = 10)
    private String phoneOtp;

    private LocalDateTime phoneOtpExpiry;

    @Column(length = 100)
    private String resetToken;

    private LocalDateTime resetTokenExpiry;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<StudentCourse> enrolledCourses = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Notification> notifications = new HashSet<>();

    public boolean isFullyVerified() {
        return emailVerified && (phone == null || phone.isBlank() || phoneVerified);
    }

    public enum Role {
        STUDENT, ADMIN
    }
}
