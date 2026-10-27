package com.nptelmanager.controller;

import com.nptelmanager.entity.*;
import com.nptelmanager.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final UserService userService;
    private final AssignmentService assignmentService;
    private final ExaminationService examinationService;

    @GetMapping
    public String calendar(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = userService.getByEmail(userDetails.getUsername());

        // Build calendar events list as JSON-compatible map list
        List<Map<String, Object>> events = new ArrayList<>();

        // Assignment deadlines
        List<StudentAssignment> assignments = assignmentService.getStudentAssignments(student);
        for (StudentAssignment sa : assignments) {
            if (sa.getAssignment().getDueDate() != null) {
                Map<String, Object> event = new HashMap<>();
                event.put("title", "📝 " + sa.getAssignment().getTitle());
                event.put("start", sa.getAssignment().getDueDate().toLocalDate().toString());
                event.put("color", getAssignmentColor(sa.getStatus()));
                event.put("url", "/assignments");
                event.put("extendedProps", Map.of(
                    "type", "assignment",
                    "course", sa.getAssignment().getCourse().getCourseName(),
                    "status", sa.getStatus().name()
                ));
                events.add(event);
            }
        }

        // Exam registrations and exam dates
        List<ExaminationRegistration> examRegs = examinationService.getStudentRegistrations(student);
        for (ExaminationRegistration reg : examRegs) {
            Examination exam = reg.getExamination();

            if (exam.getRegistrationOpenDate() != null) {
                Map<String, Object> e = new HashMap<>();
                e.put("title", "📋 Reg Opens: " + exam.getCourse().getCourseName());
                e.put("start", exam.getRegistrationOpenDate().toString());
                e.put("color", "#17a2b8");
                e.put("url", "/examinations/" + exam.getId());
                events.add(e);
            }
            if (exam.getRegistrationCloseDate() != null) {
                Map<String, Object> e = new HashMap<>();
                e.put("title", "⏰ Reg Closes: " + exam.getCourse().getCourseName());
                e.put("start", exam.getRegistrationCloseDate().toString());
                e.put("color", "#fd7e14");
                e.put("url", "/examinations/" + exam.getId());
                events.add(e);
            }
            if (exam.getExamDate() != null) {
                Map<String, Object> e = new HashMap<>();
                e.put("title", "🎓 EXAM: " + exam.getCourse().getCourseName());
                e.put("start", exam.getExamDate().toString());
                e.put("color", "#dc3545");
                e.put("url", "/examinations/" + exam.getId());
                events.add(e);
            }
        }

        // Convert events to JSON string
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            Map<String, Object> ev = events.get(i);
            sb.append("{");
            sb.append("\"title\":\"").append(escapeJson(ev.get("title").toString())).append("\",");
            sb.append("\"start\":\"").append(ev.get("start")).append("\",");
            sb.append("\"color\":\"").append(ev.get("color")).append("\",");
            sb.append("\"url\":\"").append(ev.get("url")).append("\"");
            sb.append("}");
            if (i < events.size() - 1) sb.append(",");
        }
        sb.append("]");

        model.addAttribute("eventsJson", sb.toString());
        model.addAttribute("student", student);
        return "calendar/calendar";
    }

    private String getAssignmentColor(StudentAssignment.AssignmentStatus status) {
        return switch (status) {
            case COMPLETED -> "#28a745";
            case OVERDUE -> "#dc3545";
            case IN_PROGRESS -> "#ffc107";
            default -> "#6c757d";
        };
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
