package com.nptelmanager.controller;

import com.nptelmanager.entity.User;
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

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("errorMsg", "Invalid email or password.");
        if (logout != null) model.addAttribute("logoutMsg", "You have been logged out successfully.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user,
                           BindingResult result,
                           RedirectAttributes redirectAttrs,
                           Model model) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(user);
            redirectAttrs.addFlashAttribute("successMsg",
                "Account created successfully! Please login with your credentials.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Registration error: " + (e.getMessage() != null ? e.getMessage() : "Please check your inputs and try again."));
            return "auth/register";
        }
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
