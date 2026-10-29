package com.nptelmanager.controller;

import com.nptelmanager.entity.User;
import com.nptelmanager.service.TamilNaduCollegeService;
import com.nptelmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserService userService;
    private final TamilNaduCollegeService collegeService;

    @GetMapping
    public String settingsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = userService.getByEmail(userDetails.getUsername());
        model.addAttribute("student", student);
        model.addAttribute("colleges", collegeService.getAllColleges());
        return "settings/settings";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @ModelAttribute User updatedUser,
                                RedirectAttributes redirectAttrs) {
        User current = userService.getByEmail(userDetails.getUsername());
        try {
            userService.updateProfile(current.getId(), updatedUser);
            redirectAttrs.addFlashAttribute("successMsg", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", "Failed to update profile: " + e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttrs) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttrs.addFlashAttribute("errorMsg", "New passwords do not match.");
            return "redirect:/settings";
        }
        if (newPassword.length() < 6) {
            redirectAttrs.addFlashAttribute("errorMsg", "Password must be at least 6 characters.");
            return "redirect:/settings";
        }
        User current = userService.getByEmail(userDetails.getUsername());
        try {
            userService.changePassword(current.getId(), currentPassword, newPassword);
            redirectAttrs.addFlashAttribute("successMsg", "Password changed successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/settings";
    }
}
