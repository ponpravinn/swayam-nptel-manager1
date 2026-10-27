package com.nptelmanager.service;

import com.nptelmanager.entity.Notification;
import com.nptelmanager.entity.User;
import com.nptelmanager.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.admin.email:admin@nptelmanager.com}")
    private String adminEmail;

    /**
     * Create and persist a notification. Uses deduplication key to prevent duplicates.
     */
    public Notification createNotification(User user, String title, String message,
                                            Notification.NotificationType type, String dedupKey,
                                            Long relatedAssignmentId, Long relatedExaminationId,
                                            Long relatedCourseId) {
        if (dedupKey != null && notificationRepository.existsByDeduplicationKey(dedupKey)) {
            log.debug("Skipping duplicate notification: {}", dedupKey);
            return null;
        }

        Notification notification = Notification.builder()
            .user(user)
            .title(title)
            .message(message)
            .type(type)
            .isRead(false)
            .deduplicationKey(dedupKey)
            .relatedAssignmentId(relatedAssignmentId)
            .relatedExaminationId(relatedExaminationId)
            .relatedCourseId(relatedCourseId)
            .build();

        Notification saved = notificationRepository.save(notification);

        // Send email if enabled and student has it enabled
        if (emailEnabled && user.isEmailNotificationsEnabled()) {
            sendEmail(user.getEmail(), title != null ? title : "NPTEL Manager Notification", message);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(Long userId) {
        return notificationRepository.findRecentByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    public void markAllAsRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[NPTEL Manager] " + subject);
            message.setText(body);
            message.setFrom(adminEmail);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
