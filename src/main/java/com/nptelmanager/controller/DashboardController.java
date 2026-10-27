package com.nptelmanager.controller;

import com.nptelmanager.entity.*;
import com.nptelmanager.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final ExaminationService examinationService;
    private final NotificationService notificationService;

    @GetMapping({"", "/"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = userService.getByEmail(userDetails.getUsername());

        // Sync overdue statuses on dashboard view
        assignmentService.syncOverdueStatuses(student);

        // --- Course stats ---
        List<StudentCourse> courses = courseService.getStudentCourses(student);
        model.addAttribute("totalCourses", courses.size());

        // --- Assignment stats ---
        List<StudentAssignment> allAssignments = assignmentService.getStudentAssignments(student);
        long total = allAssignments.size();
        long completed = allAssignments.stream()
            .filter(sa -> sa.getStatus() == StudentAssignment.AssignmentStatus.COMPLETED).count();
        long inProgress = allAssignments.stream()
            .filter(sa -> sa.getStatus() == StudentAssignment.AssignmentStatus.IN_PROGRESS).count();
        long notStarted = allAssignments.stream()
            .filter(sa -> sa.getStatus() == StudentAssignment.AssignmentStatus.NOT_STARTED).count();
        long overdue = allAssignments.stream()
            .filter(sa -> sa.isEffectivelyOverdue()).count();
        long incomplete = total - completed;

        model.addAttribute("totalAssignments", total);
        model.addAttribute("completedAssignments", completed);
        model.addAttribute("incompleteAssignments", incomplete);
        model.addAttribute("overdueAssignments", overdue);
        model.addAttribute("inProgressAssignments", inProgress);
        int completionPercent = total > 0 ? (int) ((completed * 100) / total) : 0;
        model.addAttribute("completionPercent", completionPercent);

        // Due today
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        List<StudentAssignment> dueToday = allAssignments.stream()
            .filter(sa -> sa.getStatus() != StudentAssignment.AssignmentStatus.COMPLETED
                && sa.getAssignment().getDueDate() != null
                && !sa.getAssignment().getDueDate().isBefore(startOfDay)
                && !sa.getAssignment().getDueDate().isAfter(endOfDay))
            .collect(Collectors.toList());
        model.addAttribute("dueToday", dueToday);

        // Due in next 7 days
        LocalDateTime in7Days = LocalDateTime.now().plusDays(7);
        List<StudentAssignment> dueSoon = allAssignments.stream()
            .filter(sa -> sa.getStatus() != StudentAssignment.AssignmentStatus.COMPLETED
                && sa.getAssignment().getDueDate() != null
                && sa.getAssignment().getDueDate().isAfter(LocalDateTime.now())
                && sa.getAssignment().getDueDate().isBefore(in7Days))
            .sorted((a, b) -> a.getAssignment().getDueDate().compareTo(b.getAssignment().getDueDate()))
            .collect(Collectors.toList());
        model.addAttribute("dueSoon", dueSoon);

        // Recently completed
        List<StudentAssignment> recentlyCompleted = allAssignments.stream()
            .filter(sa -> sa.getStatus() == StudentAssignment.AssignmentStatus.COMPLETED)
            .sorted((a, b) -> {
                if (a.getCompletedAt() == null) return 1;
                if (b.getCompletedAt() == null) return -1;
                return b.getCompletedAt().compareTo(a.getCompletedAt());
            })
            .limit(5)
            .collect(Collectors.toList());
        model.addAttribute("recentlyCompleted", recentlyCompleted);

        // --- Examination stats ---
        List<ExaminationRegistration> examRegs = examinationService.getStudentRegistrations(student);
        long upcomingExamRegs = examRegs.stream()
            .filter(r -> r.getRegistrationStatus() == ExaminationRegistration.RegistrationStatus.REGISTRATION_OPEN).count();
        long upcomingExams = examRegs.stream()
            .filter(r -> r.getExamination().getExamDate() != null
                && !r.getExamination().getExamDate().isBefore(java.time.LocalDate.now())).count();
        model.addAttribute("upcomingExamRegistrations", upcomingExamRegs);
        model.addAttribute("upcomingExams", upcomingExams);
        model.addAttribute("examRegistrations", examRegs);

        // --- Notifications ---
        List<Notification> recentNotifications = notificationService.getRecentNotifications(student.getId());
        long unreadCount = notificationService.getUnreadCount(student.getId());
        model.addAttribute("recentNotifications", recentNotifications);
        model.addAttribute("unreadCount", unreadCount);

        model.addAttribute("student", student);
        model.addAttribute("courses", courses);

        // Chart data (JSON-ready)
        model.addAttribute("chartCompleted", completed);
        model.addAttribute("chartIncomplete", incomplete);

        // Course-wise chart data
        List<String> courseLabels = new java.util.ArrayList<>();
        List<Integer> courseCompletedCounts = new java.util.ArrayList<>();
        List<Integer> courseTotalCounts = new java.util.ArrayList<>();

        for (StudentCourse sc : courses) {
            Course c = sc.getCourse();
            long cTotal = allAssignments.stream()
                .filter(sa -> sa.getAssignment().getCourse().getId().equals(c.getId()))
                .count();
            long cCompleted = allAssignments.stream()
                .filter(sa -> sa.getAssignment().getCourse().getId().equals(c.getId())
                    && sa.getStatus() == StudentAssignment.AssignmentStatus.COMPLETED)
                .count();
            courseLabels.add(c.getCourseCode() != null ? c.getCourseCode() : c.getCourseName());
            courseCompletedCounts.add((int) cCompleted);
            courseTotalCounts.add((int) cTotal);
        }

        model.addAttribute("courseLabels", courseLabels);
        model.addAttribute("courseCompletedCounts", courseCompletedCounts);
        model.addAttribute("courseTotalCounts", courseTotalCounts);

        return "dashboard/dashboard";
    }
}
