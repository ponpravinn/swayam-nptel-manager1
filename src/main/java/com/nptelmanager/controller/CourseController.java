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
import java.util.List;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final UserService userService;
    private final CourseService courseService;

    @GetMapping
    public String listCourses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = userService.getByEmail(userDetails.getUsername());
        List<StudentCourse> myCourses = courseService.getStudentCourses(student);
        List<Course> adminCourses = courseService.getAllAdminCourses();
        java.util.Set<Long> enrolledCourseIds = myCourses.stream()
            .map(sc -> sc.getCourse().getId())
            .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("myCourses", myCourses);
        model.addAttribute("adminCourses", adminCourses);
        model.addAttribute("enrolledCourseIds", enrolledCourseIds);
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
            redirectAttrs.addFlashAttribute("successMsg", "Enrolled in course successfully!");
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
}
