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

import java.util.List;

@Controller
@RequestMapping("/examinations")
@RequiredArgsConstructor
public class ExaminationController {

    private final UserService userService;
    private final ExaminationService examinationService;
    private final CourseService courseService;
    private final WhatsAppService whatsAppService;

    @GetMapping
    public String listExaminations(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = userService.getByEmail(userDetails.getUsername());
        examinationService.syncRegistrationStatuses();

        List<ExaminationRegistration> myRegistrations = examinationService.getStudentRegistrations(student);
        List<Examination> allExams = examinationService.getAllPublished();

        model.addAttribute("myRegistrations", myRegistrations);
        model.addAttribute("allExams", allExams);
        model.addAttribute("student", student);
        return "examinations/list";
    }

    @GetMapping("/{id}")
    public String viewExamination(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        User student = userService.getByEmail(userDetails.getUsername());
        Examination exam = examinationService.findById(id)
            .orElseThrow(() -> new RuntimeException("Examination not found"));
        ExaminationRegistration myReg = examinationService.getStudentRegistration(student, id).orElse(null);

        model.addAttribute("exam", exam);
        model.addAttribute("myReg", myReg);
        model.addAttribute("student", student);
        return "examinations/detail";
    }

    @PostMapping("/track/{examId}")
    public String trackExam(@PathVariable Long examId,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttrs) {
        User student = userService.getByEmail(userDetails.getUsername());
        examinationService.enrollStudentInExam(student, examId);
        redirectAttrs.addFlashAttribute("successMsg", "Examination added to your tracker!");
        return "redirect:/examinations";
    }

    @PostMapping("/registration-status/{examId}")
    public String updateRegStatus(@PathVariable Long examId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam String status,
                                  RedirectAttributes redirectAttrs) {
        User student = userService.getByEmail(userDetails.getUsername());
        ExaminationRegistration.RegistrationStatus regStatus =
            ExaminationRegistration.RegistrationStatus.valueOf(status);
        examinationService.updateRegistrationStatus(student, examId, regStatus);
        redirectAttrs.addFlashAttribute("successMsg", "Registration status updated!");
        return "redirect:/examinations";
    }

    @PostMapping("/payment-status/{examId}")
    public String updatePaymentStatus(@PathVariable Long examId,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      @RequestParam String status,
                                      RedirectAttributes redirectAttrs) {
        User student = userService.getByEmail(userDetails.getUsername());
        ExaminationRegistration.PaymentStatus payStatus =
            ExaminationRegistration.PaymentStatus.valueOf(status);
        examinationService.updatePaymentStatus(student, examId, payStatus);
        redirectAttrs.addFlashAttribute("successMsg", "Payment status updated!");
        return "redirect:/examinations";
    }

    @GetMapping("/share-whatsapp/{id}")
    public String shareExamOnWhatsApp(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User student = userService.getByEmail(userDetails.getUsername());
        Examination exam = examinationService.findById(id).orElse(null);
        if (exam == null) {
            return "redirect:/examinations";
        }
        String text = whatsAppService.formatExamShare(exam);
        String url = whatsAppService.buildClickToChatUrl(student.getPhone(), text);
        return "redirect:" + url;
    }
}
