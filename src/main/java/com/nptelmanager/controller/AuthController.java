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
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                        @RequestParam String newPassword,
                                        @RequestParam String confirmPassword,
                                        RedirectAttributes redirectAttrs,
                                        Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMsg", "Passwords do not match.");
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }
        if (newPassword.length() < 6) {
            model.addAttribute("errorMsg", "Password must be at least 6 characters.");
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }
        try {
            userService.resetPassword(email, newPassword);
            redirectAttrs.addFlashAttribute("successMsg", "Password reset successfully! Please log in with your new password.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }
    }
}
