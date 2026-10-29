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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final UserService userService;
    private final CourseService courseService;
    private final NptelSyncService nptelSyncService;
    private final NptelScoringService nptelScoringService;
    private final AssignmentService assignmentService;

    @GetMapping
    public String listCourses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = userService.getByEmail(userDetails.getUsername());
        List<StudentCourse> myCourses = courseService.getStudentCourses(student);
        List<Course> adminCourses = courseService.getAllAdminCourses();
        java.util.Set<Long> enrolledCourseIds = myCourses.stream()
            .map(sc -> sc.getCourse().getId())
            .collect(java.util.stream.Collectors.toSet());

        Map<Long, Long> assignmentCounts = new HashMap<>();
        Map<Long, Examination> examsMap = new HashMap<>();
        for (Course c : adminCourses) {
            assignmentCounts.put(c.getId(), courseService.getAssignmentCount(c.getId()));
            courseService.getExamForCourse(c.getId()).ifPresent(e -> examsMap.put(c.getId(), e));
        }

        model.addAttribute("myCourses", myCourses);
        model.addAttribute("adminCourses", adminCourses);
        model.addAttribute("enrolledCourseIds", enrolledCourseIds);
        model.addAttribute("assignmentCounts", assignmentCounts);
        model.addAttribute("examsMap", examsMap);
        model.addAttribute("masterCatalog", nptelSyncService.getMasterCatalog());
        model.addAttribute("student", student);
        return "courses/list";
    }

    @GetMapping("/add")
    public String addCourseForm() {
        return "redirect:/courses";
    }

    @PostMapping("/add")
    public String addCourse() {
        return "redirect:/courses";
    }

    @PostMapping("/enroll/{courseId}")
    public String enrollInAdminCourse(@AuthenticationPrincipal UserDetails userDetails,
                                      @PathVariable Long courseId,
                                      RedirectAttributes redirectAttrs) {
        User student = userService.getByEmail(userDetails.getUsername());
        try {
            Course course = courseService.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            courseService.enrollInCourse(student, course);
            long count = courseService.getAssignmentCount(courseId);
            redirectAttrs.addFlashAttribute("successMsg",
                "Successfully enrolled in '" + course.getCourseName() + "'! " + count + " weekly assignments and exam details are now synced to your dashboard.");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/courses";
    }

    @GetMapping("/edit/{courseId}")
    public String editCourseForm(@PathVariable Long courseId, Model model) {
        Course course = courseService.findById(courseId)
            .orElseThrow(() -> new RuntimeException("Course not found"));
        model.addAttribute("course", course);
        model.addAttribute("pageTitle", "Edit Course");
        return "courses/form";
    }

    @PostMapping("/edit/{courseId}")
    public String editCourse(@PathVariable Long courseId,
                             @ModelAttribute Course course,
                             RedirectAttributes redirectAttrs) {
        course.setId(courseId);
        courseService.saveCourse(course);
        redirectAttrs.addFlashAttribute("successMsg", "Course updated successfully!");
        return "redirect:/courses";
    }

    @PostMapping("/delete/{courseId}")
    public String deleteCourse(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Long courseId,
                               RedirectAttributes redirectAttrs) {
        User student = userService.getByEmail(userDetails.getUsername());
        courseService.unenroll(student, courseId);
        redirectAttrs.addFlashAttribute("successMsg", "Course removed from your list.");
        return "redirect:/courses";
    }

    @PostMapping("/sync-nptel")
    public String syncNptelCourse(@AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam String nptelInput,
                                  RedirectAttributes ra) {
        User student = userService.getByEmail(userDetails.getUsername());
        try {
            Course course = nptelSyncService.syncAndPublishCourse(nptelInput);
            try {
                courseService.enrollInCourse(student, course);
                ra.addFlashAttribute("successMsg", "Successfully imported and enrolled in official SWAYAM NPTEL course: '" 
                        + course.getCourseName() + "' (" + course.getCourseCode() + ") with all weekly assignments & exam schedule!");
            } catch (IllegalArgumentException alreadyEnrolled) {
                ra.addFlashAttribute("successMsg", "Course '" + course.getCourseName() + "' is already enrolled and up to date!");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Failed to sync NPTEL course: " + e.getMessage());
        }
        return "redirect:/courses";
    }

    @GetMapping("/score-calculator")
    public String generalScoreCalculator(@AuthenticationPrincipal UserDetails userDetails,
                                         RedirectAttributes ra) {
        User student = userService.getByEmail(userDetails.getUsername());
        List<StudentCourse> myCourses = courseService.getStudentCourses(student);
        if (!myCourses.isEmpty()) {
            Long firstCourseId = myCourses.get(0).getCourse().getId();
            return "redirect:/courses/" + firstCourseId + "/score-calculator";
        }
        ra.addFlashAttribute("errorMsg", "Please enroll in at least one NPTEL course to predict your score and certificate medal!");
        return "redirect:/courses";
    }

    @GetMapping("/{id}/score-calculator")
    public String scoreCalculator(@PathVariable Long id,
                                  @RequestParam(required = false) Double examScore,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        User student = userService.getByEmail(userDetails.getUsername());
        Course course = courseService.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        List<StudentAssignment> assignments = assignmentService.getStudentAssignments(student).stream()
                .filter(sa -> sa.getAssignment().getCourse().getId().equals(id))
                .toList();

        var scoreSummary = nptelScoringService.calculateScore(course, assignments, examScore);

        model.addAttribute("course", course);
        model.addAttribute("assignments", assignments);
        model.addAttribute("scoreSummary", scoreSummary);
        model.addAttribute("simulatedExamScore", examScore);
        model.addAttribute("student", student);
        return "courses/score-calculator";
    }
}
