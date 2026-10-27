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
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final UserService userService;
    private final NotificationService notificationService;

    @GetMapping
    public String listNotifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = userService.getByEmail(userDetails.getUsername());
        List<Notification> notifications = notificationService.getNotificationsForUser(student.getId());
        long unreadCount = notificationService.getUnreadCount(student.getId());
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("student", student);
        return "notifications/list";
    }

    @PostMapping("/read/{id}")
    public String markRead(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttrs) {
        User student = userService.getByEmail(userDetails.getUsername());
        notificationService.markAsRead(id, student.getId());
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllRead(@AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttrs) {
        User student = userService.getByEmail(userDetails.getUsername());
        notificationService.markAllAsRead(student.getId());
        redirectAttrs.addFlashAttribute("successMsg", "All notifications marked as read.");
        return "redirect:/notifications";
    }
}
