package com.nptelmanager.controller;

import com.nptelmanager.entity.User;
import com.nptelmanager.service.TamilNaduCollegeService;
import com.nptelmanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final TamilNaduCollegeService collegeService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "Invalid email or password. If you recently registered, please ensure your Email and Phone Number OTPs have been verified.");
        }
        if (logout != null) model.addAttribute("logoutMsg", "You have been logged out successfully.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("colleges", collegeService.getAllColleges());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user,
                           BindingResult result,
                           RedirectAttributes redirectAttrs,
                           Model model) {
        if (result.hasErrors()) {
            model.addAttribute("colleges", collegeService.getAllColleges());
            return "auth/register";
        }
        try {
            User registered = userService.register(user);
            redirectAttrs.addFlashAttribute("successMsg",
                "Account created! Please enter the 6-digit OTP verification codes sent to your Email and Phone number to activate your account.");
            return "redirect:/auth/verify-otp?email=" + URLEncoder.encode(registered.getEmail(), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("colleges", collegeService.getAllColleges());
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Registration error: " + (e.getMessage() != null ? e.getMessage() : "Please check your inputs and try again."));
            model.addAttribute("colleges", collegeService.getAllColleges());
            return "auth/register";
        }
    }

    @GetMapping("/verify-otp")
    public String verifyOtpPage(@RequestParam(required = false) String email, Model model) {
        if (email == null || email.isBlank()) {
            return "auth/verify-otp";
        }
        String cleanEmail = email.trim().toLowerCase();
        var userOpt = userService.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            model.addAttribute("errorMsg", "No account found with email: " + cleanEmail);
            return "auth/verify-otp";
        }
        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("email", cleanEmail);
        model.addAttribute("emailVerified", user.isEmailVerified());
        model.addAttribute("phoneVerified", user.isPhoneVerified());
        model.addAttribute("hasPhone", user.getPhone() != null && !user.getPhone().isBlank());
        model.addAttribute("isFullyVerified", user.isFullyVerified());
        model.addAttribute("devEmailOtp", user.getEmailOtp());
        model.addAttribute("devPhoneOtp", user.getPhoneOtp());
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp/email")
    public String verifyEmailOtp(@RequestParam String email,
                                 @RequestParam String otp,
                                 RedirectAttributes redirectAttrs) {
        if (email == null || email.isBlank()) {
            return "redirect:/auth/register";
        }
        String cleanEmail = email.trim().toLowerCase();
        try {
            userService.verifyEmailOtp(cleanEmail, otp);
            User user = userService.getByEmail(cleanEmail);
            if (user.isFullyVerified()) {
                redirectAttrs.addFlashAttribute("successMsg", "Email verified successfully! All contact details are confirmed. You can now log in.");
                return "redirect:/auth/login";
            } else {
                redirectAttrs.addFlashAttribute("emailSuccessMsg", "Email verified successfully! Please now verify your Phone Number OTP below.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("emailErrorMsg", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("emailErrorMsg", "Verification error: " + e.getMessage());
        }
        return "redirect:/auth/verify-otp?email=" + URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8);
    }

    @PostMapping("/verify-otp/phone")
    public String verifyPhoneOtp(@RequestParam String email,
                                 @RequestParam String otp,
                                 RedirectAttributes redirectAttrs) {
        if (email == null || email.isBlank()) {
            return "redirect:/auth/register";
        }
        String cleanEmail = email.trim().toLowerCase();
        try {
            userService.verifyPhoneOtp(cleanEmail, otp);
            User user = userService.getByEmail(cleanEmail);
            if (user.isFullyVerified()) {
                redirectAttrs.addFlashAttribute("successMsg", "Phone number verified successfully! All contact details are confirmed. You can now log in.");
                return "redirect:/auth/login";
            } else {
                redirectAttrs.addFlashAttribute("phoneSuccessMsg", "Phone number verified successfully! Please ensure your Email OTP is also verified.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("phoneErrorMsg", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("phoneErrorMsg", "Verification error: " + e.getMessage());
        }
        return "redirect:/auth/verify-otp?email=" + URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8);
    }

    @PostMapping("/verify-otp/resend")
    public String resendOtp(@RequestParam String email,
                            @RequestParam String type,
                            RedirectAttributes redirectAttrs) {
        if (email == null || email.isBlank()) {
            return "redirect:/auth/register";
        }
        String cleanEmail = email.trim().toLowerCase();
        try {
            userService.resendOtp(cleanEmail, type);
            if ("email".equalsIgnoreCase(type)) {
                redirectAttrs.addFlashAttribute("emailSuccessMsg", "A new 6-digit Email OTP has been sent!");
            } else {
                redirectAttrs.addFlashAttribute("phoneSuccessMsg", "A new 6-digit Phone OTP has been sent via WhatsApp/SMS!");
            }
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", "Failed to resend OTP: " + e.getMessage());
        }
        return "redirect:/auth/verify-otp?email=" + URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8);
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                        RedirectAttributes redirectAttrs,
                                        Model model) {
        if (email == null || email.isBlank()) {
            model.addAttribute("errorMsg", "Please enter your registered email address.");
            return "auth/forgot-password";
        }
        try {
            String otp = userService.generatePasswordResetOtp(email.trim());
            redirectAttrs.addFlashAttribute("successMsg", "A 6-digit verification OTP has been generated and sent to your email (valid for 15 minutes).");
            redirectAttrs.addFlashAttribute("email", email.trim());
            redirectAttrs.addFlashAttribute("devOtpNotice", "Dev/Demo Notice: Generated OTP is " + otp);
            return "redirect:/auth/reset-password?email=" + URLEncoder.encode(email.trim(), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String email, Model model) {
        if (email != null && !model.containsAttribute("email")) {
            model.addAttribute("email", email);
        }
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String email,
                                       @RequestParam String otp,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       RedirectAttributes redirectAttrs,
                                       Model model) {
        model.addAttribute("email", email);
        if (otp == null || otp.isBlank()) {
            model.addAttribute("errorMsg", "Please enter the 6-digit OTP code.");
            return "auth/reset-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            return "auth/reset-password";
        }
        if (newPassword.length() < 6) {
            model.addAttribute("errorMsg", "Password must be at least 6 characters.");
            return "auth/reset-password";
        }
        try {
            userService.verifyAndResetPassword(email.trim(), otp.trim(), newPassword);
            redirectAttrs.addFlashAttribute("successMsg", "Password reset successfully! Please log in with your new password.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/reset-password";
        }
    }
}
