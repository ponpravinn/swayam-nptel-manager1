package com.nptelmanager.controller;

import com.nptelmanager.entity.*;
import com.nptelmanager.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class NotificationController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final WhatsAppService whatsAppService;

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

    @GetMapping("/whatsapp/{id}")
    public String openInWhatsApp(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User student = userService.getByEmail(userDetails.getUsername());
        Notification n = notificationService.findById(id);
        if (n == null) {
            return "redirect:/notifications";
        }

        String phone = student.getPhone();
        String message = whatsAppService.formatWhatsAppNotification(n.getTitle(), n.getMessage(), student.getFullName());
        String url = whatsAppService.buildClickToChatUrl(phone, message);
        return "redirect:" + url;
    }

    @PostMapping("/test-whatsapp")
    public String sendTestWhatsApp(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam(required = false) String phone,
                                   RedirectAttributes ra) {
        User student = userService.getByEmail(userDetails.getUsername());
        String targetPhone = (phone != null && !phone.isBlank()) ? phone : student.getPhone();

        if (targetPhone == null || targetPhone.isBlank()) {
            ra.addFlashAttribute("errorMsg", "Please provide a valid phone number in your profile to receive WhatsApp alerts.");
            return "redirect:/settings";
        }

        String title = "Test Notification: Deadline Alert Ready";
        String body = "This is a test notification from your SWAYAM NPTEL Assignment Manager. Automated assignment reminders and exam updates will arrive here!";

        whatsAppService.dispatch(
                whatsAppService.cleanPhoneNumber(targetPhone),
                whatsAppService.formatWhatsAppNotification(title, body, student.getFullName()),
                title,
                student.getFullName()
        );

        String waUrl = whatsAppService.buildClickToChatUrl(
                targetPhone,
                whatsAppService.formatWhatsAppNotification(title, body, student.getFullName())
        );

        ra.addFlashAttribute("successMsg", "WhatsApp alert processed! Click 'Open in WhatsApp' to view the message in your WhatsApp chat.");
        ra.addFlashAttribute("whatsappDirectUrl", waUrl);
        ra.addFlashAttribute("whatsappTargetPhone", targetPhone);

        return "redirect:/settings";
    }
}
