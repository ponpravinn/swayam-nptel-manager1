package com.nptelmanager.service;

import com.nptelmanager.entity.*;
import com.nptelmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled service that runs every hour to send assignment and examination reminders.
 * Uses deduplication keys to prevent duplicate notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSchedulerService {

    private final StudentAssignmentRepository studentAssignmentRepo;
    private final ExaminationRegistrationRepository examRegRepo;
    private final NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Value("${app.notification.reminder-days:7,3,1,0}")
    private String assignmentReminderDaysConfig;

    @org.springframework.beans.factory.annotation.Value("${app.notification.exam-reminder-days:7,3,1,0}")
    private String examReminderDaysConfig;

    private static final int[] DEFAULT_DAYS = {7, 3, 1, 0};

    private int[] parseDays(String config) {
        if (config == null || config.isBlank()) return DEFAULT_DAYS;
        try {
            return java.util.Arrays.stream(config.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .toArray();
        } catch (Exception e) {
            log.warn("Failed to parse reminder days config '{}', falling back to defaults", config, e);
            return DEFAULT_DAYS;
        }
    }

    /**
     * Runs every hour to check for upcoming assignment deadlines.
     */
    @Scheduled(fixedDelay = 3600000, initialDelay = 30000) // every hour, start after 30s
    @Transactional(readOnly = true)
    public void checkAssignmentReminders() {
        log.debug("Running assignment reminder check...");
        List<StudentAssignment> allIncomplete = studentAssignmentRepo.findAll().stream()
            .filter(sa -> sa.getStatus() != StudentAssignment.AssignmentStatus.COMPLETED)
            .toList();

        for (StudentAssignment sa : allIncomplete) {
            User student = sa.getStudent();
            Assignment assignment = sa.getAssignment();
            LocalDateTime dueDate = assignment.getDueDate();

            if (dueDate == null) continue;

            long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate.toLocalDate());

            // Overdue check
            if (daysUntilDue < 0) {
                String dedupKey = "OVERDUE-" + student.getId() + "-" + assignment.getId() + "-" + LocalDate.now();
                notificationService.createNotification(
                    student,
                    "Assignment Overdue",
                    String.format("Your NPTEL assignment '%s' for course '%s' deadline has passed. Please check if submission is still available.",
                        assignment.getTitle(), assignment.getCourse().getCourseName()),
                    Notification.NotificationType.ASSIGNMENT_OVERDUE,
                    dedupKey,
                    assignment.getId(), null, assignment.getCourse().getId()
                );
                continue;
            }

            // Upcoming reminders
            for (int reminderDay : parseDays(assignmentReminderDaysConfig)) {
                if (daysUntilDue == reminderDay) {
                    String dedupKey = "ASSIGN-" + student.getId() + "-" + assignment.getId() + "-D" + reminderDay;
                    String msg = buildAssignmentReminderMessage(assignment, reminderDay);
                    notificationService.createNotification(
                        student,
                        "Assignment Reminder",
                        msg,
                        Notification.NotificationType.ASSIGNMENT_REMINDER,
                        dedupKey,
                        assignment.getId(), null, assignment.getCourse().getId()
                    );
                }
            }
        }
        log.debug("Assignment reminder check complete.");
    }

    /**
     * Runs every hour to check for examination registration and exam date reminders.
     */
    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    @Transactional(readOnly = true)
    public void checkExaminationReminders() {
        log.debug("Running examination reminder check...");
        List<ExaminationRegistration> registrations = examRegRepo.findAll();

        for (ExaminationRegistration reg : registrations) {
            User student = reg.getStudent();
            Examination exam = reg.getExamination();

            // Registration deadline reminders (if not yet registered)
            if (reg.getRegistrationStatus() != ExaminationRegistration.RegistrationStatus.REGISTERED
                && exam.getRegistrationCloseDate() != null) {
                LocalDate closeDate = exam.getRegistrationCloseDate();
                long daysToClose = ChronoUnit.DAYS.between(LocalDate.now(), closeDate);

                for (int d : parseDays(examReminderDaysConfig)) {
                    if (daysToClose == d) {
                        String dedupKey = "EXAM-REG-" + student.getId() + "-" + exam.getId() + "-D" + d;
                        String msg = d == 0
                            ? String.format("TODAY is the last day to register for the %s exam for '%s'. Register now at: %s",
                                exam.getExamSession(), exam.getCourse().getCourseName(), exam.getRegistrationUrl())
                            : String.format("Registration for %s exam ('%s') closes in %d day(s). Register at: %s",
                                exam.getExamSession(), exam.getCourse().getCourseName(), d, exam.getRegistrationUrl());
                        notificationService.createNotification(
                            student, "Exam Registration Deadline",
                            msg, Notification.NotificationType.EXAM_REGISTRATION_DEADLINE,
                            dedupKey, null, exam.getId(), exam.getCourse().getId()
                        );
                    }
                }

                // Missed registration
                if (daysToClose < 0) {
                    String dedupKey = "EXAM-REG-MISSED-" + student.getId() + "-" + exam.getId() + "-" + LocalDate.now();
                    notificationService.createNotification(
                        student, "Registration Deadline Missed",
                        String.format("The registration deadline for %s exam ('%s') has passed.",
                            exam.getExamSession(), exam.getCourse().getCourseName()),
                        Notification.NotificationType.EXAM_REGISTRATION_MISSED,
                        dedupKey, null, exam.getId(), exam.getCourse().getId()
                    );
                }
            }

            // Fee payment reminder (if registered but not paid)
            if (reg.getRegistrationStatus() == ExaminationRegistration.RegistrationStatus.REGISTERED
                && reg.getPaymentStatus() == ExaminationRegistration.PaymentStatus.NOT_PAID) {
                String dedupKey = "EXAM-FEE-" + student.getId() + "-" + exam.getId() + "-" + LocalDate.now();
                notificationService.createNotification(
                    student, "Exam Fee Payment Pending",
                    String.format("You are registered for %s exam but the fee payment is pending. Please pay ₹%.2f.",
                        exam.getCourse().getCourseName(),
                        exam.getRegistrationFee() != null ? exam.getRegistrationFee().doubleValue() : 0.0),
                    Notification.NotificationType.EXAM_FEE_PENDING,
                    dedupKey, null, exam.getId(), exam.getCourse().getId()
                );
            }

            // Exam date reminders
            if (exam.getExamDate() != null) {
                long daysToExam = ChronoUnit.DAYS.between(LocalDate.now(), exam.getExamDate());
                for (int d : parseDays(examReminderDaysConfig)) {
                    if (daysToExam == d) {
                        String dedupKey = "EXAM-DATE-" + student.getId() + "-" + exam.getId() + "-D" + d;
                        String msg = d == 0
                            ? String.format("Your %s exam for '%s' is TODAY at %s. Good luck!",
                                exam.getExamSession(), exam.getCourse().getCourseName(), exam.getExamStartTime())
                            : String.format("Your %s exam for '%s' is in %d day(s) on %s at %s.",
                                exam.getExamSession(), exam.getCourse().getCourseName(), d, exam.getExamDate(), exam.getExamStartTime());
                        notificationService.createNotification(
                            student, "Exam Date Reminder",
                            msg, Notification.NotificationType.EXAM_DATE_REMINDER,
                            dedupKey, null, exam.getId(), exam.getCourse().getId()
                        );
                    }
                }
            }
        }
        log.debug("Examination reminder check complete.");
    }

    private String buildAssignmentReminderMessage(Assignment assignment, int daysLeft) {
        String courseName = assignment.getCourse().getCourseName();
        String title = assignment.getTitle();
        if (daysLeft == 0) {
            return String.format("DEADLINE TODAY: Your NPTEL assignment '%s' for '%s' is due today! Complete your submission now.", title, courseName);
        }
        return String.format("Reminder: Your NPTEL assignment '%s' for '%s' is due in %d day(s) on %s.",
            title, courseName, daysLeft, assignment.getDueDate().toLocalDate());
    }
}
