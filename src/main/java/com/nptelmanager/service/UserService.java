package com.nptelmanager.service;

import com.nptelmanager.entity.User;
import com.nptelmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:noreply@nptelmanager.com}")
    private String mailFrom;

    public User register(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(User.Role.STUDENT);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public User updateProfile(Long userId, User updatedUser) {
        User existing = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        existing.setFullName(updatedUser.getFullName());
        existing.setPhone(updatedUser.getPhone());
        existing.setCollege(updatedUser.getCollege());
        existing.setDepartment(updatedUser.getDepartment());
        existing.setRollNumber(updatedUser.getRollNumber());
        existing.setEmailNotificationsEnabled(updatedUser.isEmailNotificationsEnabled());
        return userRepository.save(existing);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public String generatePasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No account registered with email: " + email));

        int code = 100000 + new java.security.SecureRandom().nextInt(900000);
        String otp = String.valueOf(code);

        user.setResetToken(otp);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        log.info("Password reset OTP generated for {}: {}", email, otp);

        if (emailEnabled && mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(mailFrom != null && !mailFrom.isBlank() ? mailFrom : "noreply@nptelmanager.com");
                msg.setTo(email);
                msg.setSubject("[NPTEL Manager] Password Reset OTP Verification");
                msg.setText("Hello " + user.getFullName() + ",\n\n"
                    + "Your One-Time Password (OTP) for resetting your SWAYAM NPTEL Assignment Manager account password is:\n\n"
                    + "   " + otp + "\n\n"
                    + "This OTP is valid for 15 minutes. If you did not request this, please ignore this email.\n\n"
                    + "Regards,\nSWAYAM NPTEL Assignment Manager");
                mailSender.send(msg);
                log.info("Reset OTP email sent to {}", email);
            } catch (Exception e) {
                log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
            }
        }
        return otp;
    }

    public void verifyAndResetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No account registered with email: " + email));

        if (user.getResetToken() == null || user.getResetTokenExpiry() == null) {
            throw new IllegalArgumentException("No password reset was requested for this account.");
        }

        if (LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            throw new IllegalArgumentException("The OTP has expired. Please request a new one.");
        }

        if (!user.getResetToken().equals(otp.trim())) {
            throw new IllegalArgumentException("Invalid OTP code. Please check and try again.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password successfully reset for user {}", email);
    }

    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("No account registered with email: " + email));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
