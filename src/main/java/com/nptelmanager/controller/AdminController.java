package com.nptelmanager.controller;

import com.nptelmanager.entity.*;
import com.nptelmanager.repository.UserRepository;
import com.nptelmanager.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final CourseService courseService;
    private final AssignmentService assignmentService;
    private final ExaminationService examinationService;
    private final CsvImportService csvImportService;
    private final UserRepository userRepository;
    private final NptelSyncService nptelSyncService;

    @GetMapping
    public String adminDashboard(Model model) {
        List<Course> courses = courseService.getAllAdminCourses();
        Map<Long, Long> assignmentCounts = new HashMap<>();
        Map<Long, Long> studentCounts = new HashMap<>();
        Map<Long, Examination> examsMap = new HashMap<>();

        for (Course c : courses) {
            assignmentCounts.put(c.getId(), courseService.getAssignmentCount(c.getId()));
            studentCounts.put(c.getId(), courseService.getEnrolledCount(c.getId()));
            courseService.getExamForCourse(c.getId()).ifPresent(exam -> examsMap.put(c.getId(), exam));
        }

        Set<String> publishedCodes = courses.stream()
            .map(c -> c.getCourseCode().toLowerCase())
            .collect(Collectors.toSet());

        model.addAttribute("courses", courses);
        model.addAttribute("assignmentCounts", assignmentCounts);
        model.addAttribute("studentCounts", studentCounts);
        model.addAttribute("examsMap", examsMap);
        model.addAttribute("officialPresets", courseService.getOfficialPresets());
        model.addAttribute("publishedCodes", publishedCodes);
        model.addAttribute("totalStudents", userRepository.count());
        model.addAttribute("totalAssignments", assignmentService.getAllAdminAssignments().size());
        model.addAttribute("totalExams", examinationService.getAllPublished().size());
        return "admin/dashboard";
    }

    // ---- Course Management ----
    @GetMapping("/courses/add")
    public String addCourseForm(Model model) {
        Course course = new Course();
        course.setDuration("12 Weeks");
        course.setSemester("Jul-Oct 2026");
        model.addAttribute("course", course);
        return "admin/course-form";
    }

    @PostMapping("/courses/add")
    public String addCourse(@ModelAttribute Course course,
                            @RequestParam(defaultValue = "false") boolean generateAssignments,
                            @RequestParam(defaultValue = "12") Integer numberOfWeeks,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime firstAssignmentDueDate,
                            @RequestParam(defaultValue = "false") boolean scheduleExam,
                            @RequestParam(required = false) String examSession,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate examDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime examStartTime,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime examEndTime,
                            @RequestParam(required = false) BigDecimal registrationFee,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationOpenDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationCloseDate,
                            @RequestParam(required = false) String examCenter,
                            @RequestParam(required = false) String examMode,
                            RedirectAttributes ra) {
        try {
            Course saved = courseService.createFullCourse(
                course,
                generateAssignments,
                numberOfWeeks,
                firstAssignmentDueDate,
                scheduleExam,
                examSession,
                examDate,
                examStartTime,
                examEndTime,
                registrationFee,
                registrationOpenDate,
                registrationCloseDate,
                examCenter,
                examMode
            );
            String msg = "Course '" + saved.getCourseName() + "' published successfully!";
            if (generateAssignments) {
                msg += " " + numberOfWeeks + " weekly assignments created.";
            }
            if (scheduleExam) {
                msg += " Certification examination scheduled.";
            }
            ra.addFlashAttribute("successMsg", msg);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Error adding course: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/courses/quick-publish/{presetCode}")
    public String quickPublishCourse(@PathVariable String presetCode, RedirectAttributes ra) {
        try {
            Course c = courseService.publishPreset(presetCode);
            ra.addFlashAttribute("successMsg", "Official SWAYAM NPTEL course '" + c.getCourseName() + "' published successfully with all weekly assignments and examination details!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to publish course: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/courses/sync-nptel")
    public String adminSyncNptelCourse(@RequestParam String nptelInput, RedirectAttributes ra) {
        try {
            Course c = nptelSyncService.syncAndPublishCourse(nptelInput);
            ra.addFlashAttribute("successMsg", "Official SWAYAM NPTEL course '" + c.getCourseName() + "' (" + c.getCourseCode() + ") synchronized and published globally with all weekly assignments and examination details!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to sync NPTEL course: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @GetMapping("/courses/edit/{id}")
    public String editCourseForm(@PathVariable Long id, Model model) {
        Course course = courseService.findById(id)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        model.addAttribute("course", course);
        return "admin/course-form";
    }

    @PostMapping("/courses/edit/{id}")
    public String editCourse(@PathVariable Long id, @ModelAttribute Course course, RedirectAttributes ra) {
        course.setId(id);
        course.setAdminCreated(true);
        courseService.saveCourse(course);
        ra.addFlashAttribute("successMsg", "Course updated!");
        return "redirect:/admin";
    }

    @PostMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes ra) {
        courseService.deleteCourse(id);
        ra.addFlashAttribute("successMsg", "Course deleted.");
        return "redirect:/admin";
    }

    // ---- Assignment Management ----
    @GetMapping("/assignments/add")
    public String addAssignmentForm(Model model) {
        model.addAttribute("assignment", new Assignment());
        model.addAttribute("courses", courseService.getAllAdminCourses());
        return "admin/assignment-form";
    }

    @PostMapping("/assignments/add")
    public String addAssignment(@ModelAttribute Assignment assignment,
                                @RequestParam Long courseId,
                                RedirectAttributes ra) {
        Course course = courseService.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        assignment.setCourse(course);
        assignmentService.adminAddAssignment(assignment);
        ra.addFlashAttribute("successMsg", "Assignment published to all enrolled students!");
        return "redirect:/admin";
    }

    // ---- Examination Management ----
    @GetMapping("/examinations")
    public String listExams(Model model) {
        model.addAttribute("examinations", examinationService.getAllPublished());
        return "admin/examinations";
    }

    @GetMapping("/examinations/add")
    public String addExamForm(Model model) {
        model.addAttribute("examination", new Examination());
        model.addAttribute("courses", courseService.getAllAdminCourses());
        return "admin/exam-form";
    }

    @PostMapping("/examinations/add")
    public String addExam(@ModelAttribute Examination examination,
                          @RequestParam Long courseId,
                          RedirectAttributes ra) {
        Course course = courseService.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        examination.setCourse(course);
        examination.setPublished(true);
        examinationService.saveExamination(examination);
        ra.addFlashAttribute("successMsg", "Examination information published!");
        return "redirect:/admin/examinations";
    }

    @GetMapping("/examinations/edit/{id}")
    public String editExamForm(@PathVariable Long id, Model model) {
        Examination exam = examinationService.findById(id)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
        model.addAttribute("examination", exam);
        model.addAttribute("courses", courseService.getAllAdminCourses());
        return "admin/exam-form";
    }

    @PostMapping("/examinations/edit/{id}")
    public String editExam(@PathVariable Long id, @ModelAttribute Examination exam,
                           @RequestParam Long courseId, RedirectAttributes ra) {
        Course course = courseService.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        exam.setId(id);
        exam.setCourse(course);
        exam.setPublished(true);
        examinationService.saveExamination(exam);
        ra.addFlashAttribute("successMsg", "Examination updated!");
        return "redirect:/admin/examinations";
    }

    @PostMapping("/examinations/delete/{id}")
    public String deleteExam(@PathVariable Long id, RedirectAttributes ra) {
        examinationService.deleteExamination(id);
        ra.addFlashAttribute("successMsg", "Examination deleted.");
        return "redirect:/admin/examinations";
    }

    // ---- User & Database Management ----
    @GetMapping({"/users", "/database"})
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("studentCount", users.stream().filter(u -> u.getRole() == User.Role.STUDENT).count());
        model.addAttribute("adminCount", users.stream().filter(u -> u.getRole() == User.Role.ADMIN).count());
        model.addAttribute("totalCourses", courseService.getAllAdminCourses().size());
        model.addAttribute("totalAssignments", assignmentService.getAllAdminAssignments().size());
        model.addAttribute("totalExams", examinationService.getAllPublished().size());
        model.addAttribute("activePage", "admin-users");
        return "admin/users";
    }

    @GetMapping("/users/export-csv")
    public org.springframework.http.ResponseEntity<byte[]> exportUsersCsv() {
        List<User> users = userRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("User ID,Full Name,Email,Email Verified,Phone,Phone Verified,Institution / College,Department,Roll Number,Role,Status,Registered At\n");
        for (User u : users) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                u.getId() != null ? u.getId() : "",
                u.getFullName() != null ? u.getFullName().replace("\"", "\"\"") : "",
                u.getEmail() != null ? u.getEmail() : "",
                u.isEmailVerified() ? "VERIFIED" : "UNVERIFIED",
                u.getPhone() != null ? u.getPhone() : "",
                u.isPhoneVerified() ? "VERIFIED" : "UNVERIFIED",
                u.getCollege() != null ? u.getCollege().replace("\"", "\"\"") : "",
                u.getDepartment() != null ? u.getDepartment().replace("\"", "\"\"") : "",
                u.getRollNumber() != null ? u.getRollNumber() : "",
                u.getRole() != null ? u.getRole().name() : "",
                u.isEnabled() ? "Active" : "Disabled",
                u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
            ));
        }
        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"swayam_nptel_registered_users.csv\"")
            .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
            .body(bytes);
    }

    @PostMapping("/users/toggle/{id}")
    public String toggleUserEnabled(@PathVariable Long id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(u -> {
            u.setEnabled(!u.isEnabled());
            userRepository.save(u);
        });
        ra.addFlashAttribute("successMsg", "User status updated.");
        return "redirect:/admin/users";
    }

    // ---- CSV Import ----
    @GetMapping("/import")
    public String importPage(Model model) {
        return "admin/import";
    }

    @PostMapping("/import/courses")
    public String importCourses(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Please select a CSV file.");
            return "redirect:/admin/import";
        }
        CsvImportService.CsvImportResult result = csvImportService.importCourses(file);
        ra.addFlashAttribute("importResult", result);
        return "redirect:/admin/import";
    }

    @PostMapping("/import/assignments")
    public String importAssignments(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Please select a CSV file.");
            return "redirect:/admin/import";
        }
        CsvImportService.CsvImportResult result = csvImportService.importAssignments(file);
        ra.addFlashAttribute("importResult", result);
        return "redirect:/admin/import";
    }

    @PostMapping("/import/examinations")
    public String importExaminations(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "Please select a CSV file.");
            return "redirect:/admin/import";
        }
        CsvImportService.CsvImportResult result = csvImportService.importExaminations(file);
        ra.addFlashAttribute("importResult", result);
        return "redirect:/admin/import";
    }

    // ---- CSV Template Downloads ----
    @GetMapping(value = "/import/template/courses", produces = "text/csv")
    public ResponseEntity<byte[]> downloadCoursesTemplate() {
        byte[] content = csvImportService.getCoursesCsvTemplate().getBytes();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=courses-template.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(content);
    }

    @GetMapping(value = "/import/template/assignments", produces = "text/csv")
    public ResponseEntity<byte[]> downloadAssignmentsTemplate() {
        byte[] content = csvImportService.getAssignmentsCsvTemplate().getBytes();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=assignments-template.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(content);
    }

    @GetMapping(value = "/import/template/examinations", produces = "text/csv")
    public ResponseEntity<byte[]> downloadExaminationsTemplate() {
        byte[] content = csvImportService.getExaminationsCsvTemplate().getBytes();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=examinations-template.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(content);
    }
}
