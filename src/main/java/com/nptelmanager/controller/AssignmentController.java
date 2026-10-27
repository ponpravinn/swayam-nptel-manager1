package com.nptelmanager.controller;

import com.nptelmanager.entity.*;
import com.nptelmanager.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final UserService userService;
    private final AssignmentService assignmentService;
    private final CourseService courseService;

    @GetMapping
    public String listAssignments(@AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam(required = false) Long courseId,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String search,
                                  Model model) {
        User student = userService.getByEmail(userDetails.getUsername());
        assignmentService.syncOverdueStatuses(student);

        List<StudentAssignment> assignments = assignmentService.getStudentAssignments(student);

        // Filter by course
        if (courseId != null) {
            assignments = assignments.stream()
                .filter(sa -> sa.getAssignment().getCourse().getId().equals(courseId))
                .collect(Collectors.toList());
            model.addAttribute("selectedCourseId", courseId);
        }

        // Filter by status
        if (status != null && !status.isBlank()) {
            StudentAssignment.AssignmentStatus filterStatus =
                StudentAssignment.AssignmentStatus.valueOf(status.toUpperCase());
            assignments = assignments.stream()
                .filter(sa -> sa.getStatus() == filterStatus)
                .collect(Collectors.toList());
            model.addAttribute("selectedStatus", status);
        }

        // Search by title
        if (search != null && !search.isBlank()) {
            String lc = search.toLowerCase();
            assignments = assignments.stream()
                .filter(sa -> sa.getAssignment().getTitle().toLowerCase().contains(lc))
                .collect(Collectors.toList());
            model.addAttribute("search", search);
        }

        // Sort by due date
        assignments = assignments.stream()
            .sorted((a, b) -> {
                if (a.getAssignment().getDueDate() == null) return 1;
                if (b.getAssignment().getDueDate() == null) return -1;
                return a.getAssignment().getDueDate().compareTo(b.getAssignment().getDueDate());
            })
            .collect(Collectors.toList());

        model.addAttribute("assignments", assignments);
        model.addAttribute("courses", courseService.getStudentCourses(student));
        model.addAttribute("student", student);
        model.addAttribute("statuses", StudentAssignment.AssignmentStatus.values());
        return "assignments/list";
    }

    @GetMapping("/add")
    public String addForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = userService.getByEmail(userDetails.getUsername());
        model.addAttribute("assignment", new Assignment());
        model.addAttribute("courses", courseService.getStudentCourses(student));
        model.addAttribute("pageTitle", "Add Assignment");
        return "assignments/form";
    }

    @PostMapping("/add")
    public String addAssignment(@AuthenticationPrincipal UserDetails userDetails,
                                @ModelAttribute Assignment assignment,
                                @RequestParam Long courseId,
                                RedirectAttributes redirectAttrs) {
        User student = userService.getByEmail(userDetails.getUsername());
        Course course = courseService.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        assignment.setCourse(course);
        assignmentService.addAssignment(student, assignment);
        redirectAttrs.addFlashAttribute("successMsg", "Assignment added successfully!");
        return "redirect:/assignments";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        User student = userService.getByEmail(userDetails.getUsername());
        StudentAssignment sa = assignmentService.findStudentAssignment(student, id)
            .orElseThrow(() -> new RuntimeException("Assignment not found"));
        model.addAttribute("assignment", sa.getAssignment());
        model.addAttribute("courses", courseService.getStudentCourses(student));
        model.addAttribute("pageTitle", "Edit Assignment");
        return "assignments/form";
    }

    @PostMapping("/edit/{id}")
    public String editAssignment(@PathVariable Long id,
                                 @ModelAttribute Assignment assignment,
                                 @RequestParam Long courseId,
                                 RedirectAttributes redirectAttrs) {
        Course course = courseService.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        assignment.setId(id);
        assignment.setCourse(course);
        assignmentService.updateAssignment(assignment);
        redirectAttrs.addFlashAttribute("successMsg", "Assignment updated!");
        return "redirect:/assignments";
    }

    @PostMapping("/complete/{id}")
    public String markComplete(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(required = false) Double marks,
                               RedirectAttributes redirectAttrs) {
        User student = userService.getByEmail(userDetails.getUsername());
        assignmentService.markCompleted(student, id, marks);
        redirectAttrs.addFlashAttribute("successMsg", "Assignment marked as completed! 🎉");
        return "redirect:/assignments";
    }

    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam String status,
                               RedirectAttributes redirectAttrs) {
        User student = userService.getByEmail(userDetails.getUsername());
        StudentAssignment.AssignmentStatus newStatus = StudentAssignment.AssignmentStatus.valueOf(status);
        assignmentService.updateStatus(student, id, newStatus);
        redirectAttrs.addFlashAttribute("successMsg", "Assignment status updated.");
        return "redirect:/assignments";
    }

    @GetMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> exportAssignments(@AuthenticationPrincipal UserDetails userDetails) {
        User student = userService.getByEmail(userDetails.getUsername());
        List<StudentAssignment> list = assignmentService.getStudentAssignments(student);

        StringBuilder csv = new StringBuilder();
        csv.append("Course Code,Course Name,Assignment No,Title,Due Date,Status,Marks Obtained,Completed At\n");

        for (StudentAssignment sa : list) {
            String cCode = sa.getAssignment().getCourse() != null ? sa.getAssignment().getCourse().getCourseCode() : "";
            String cName = sa.getAssignment().getCourse() != null ? sa.getAssignment().getCourse().getCourseName().replace(",", " ") : "";
            String aNum = String.valueOf(sa.getAssignment().getAssignmentNumber());
            String title = sa.getAssignment().getTitle().replace(",", " ");
            String dueDate = sa.getAssignment().getDueDate() != null ? sa.getAssignment().getDueDate().toString() : "";
            String status = sa.getStatus().name();
            String marks = sa.getMarksObtained() != null ? String.valueOf(sa.getMarksObtained()) : "";
            String completedAt = sa.getCompletedAt() != null ? sa.getCompletedAt().toString() : "";

            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                cCode, cName, aNum, title, dueDate, status, marks, completedAt));
        }

        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"my_nptel_assignments.csv\"")
            .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
            .body(bytes);
    }
}
