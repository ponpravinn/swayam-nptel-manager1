package com.nptelmanager.controller;

import com.nptelmanager.service.NotificationService;
import com.nptelmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects common model attributes into every page (sidebar unread count, student info).
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final NotificationService notificationService;
    private final UserService userService;

    @ModelAttribute
    public void addGlobalAttributes(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            try {
                var student = userService.getByEmail(userDetails.getUsername());
                long unreadCount = notificationService.getUnreadCount(student.getId());
                model.addAttribute("unreadCount", unreadCount);
                model.addAttribute("student", student);
            } catch (Exception ignored) {
                model.addAttribute("unreadCount", 0L);
            }
        }
    }

    @org.springframework.web.bind.annotation.InitBinder
    public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new org.springframework.beans.propertyeditors.StringTrimmerEditor(true));
    }
}

