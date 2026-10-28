package com.nptelmanager.service;

import com.nptelmanager.entity.Assignment;
import com.nptelmanager.entity.Examination;
import com.nptelmanager.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Service to handle WhatsApp notifications for SWAYAM NPTEL students.
 * Supports:
 * 1. Twilio WhatsApp Business API (Automated)
 * 2. CallMeBot WhatsApp API (Free instant personal delivery)
 * 3. Direct Click-to-Chat URLs (https://wa.me/...)
 * 4. Audit & Console simulation for demo/viva presentations
 */
@Service
@Slf4j
public class WhatsAppService {

    @Value("${app.whatsapp.enabled:true}")
    private boolean enabled;

    @Value("${app.whatsapp.provider:auto}")
    private String provider; // 'auto', 'twilio', 'callmebot', 'mock'

    @Value("${app.whatsapp.callmebot.api-key:}")
    private String callMeBotApiKey;

    @Value("${app.whatsapp.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.whatsapp.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${app.whatsapp.twilio.from-number:whatsapp:+14155238886}")
    private String twilioFromNumber;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Send notification to student's WhatsApp number.
     * Returns true if sent or simulated successfully, false if disabled or phone missing.
     */
    public boolean sendNotification(User student, String title, String body) {
        if (!enabled) {
            log.debug("WhatsApp notifications are globally disabled.");
            return false;
        }

        if (student == null || student.getPhone() == null || student.getPhone().trim().isEmpty()) {
            log.debug("No phone number registered for student: {}", student != null ? student.getEmail() : "null");
            return false;
        }

        if (!student.isWhatsappNotificationsEnabled()) {
            log.debug("Student {} has disabled WhatsApp notifications in settings.", student.getEmail());
            return false;
        }

        String cleanPhone = cleanPhoneNumber(student.getPhone());
        String formattedMessage = formatWhatsAppNotification(title, body, student.getFullName());

        return dispatch(cleanPhone, formattedMessage, title, student.getFullName());
    }

    /**
     * Dispatch WhatsApp message to phone number via configured provider.
     */
    public boolean dispatch(String cleanPhone, String messageText, String title, String recipientName) {
        if (cleanPhone == null || cleanPhone.isEmpty()) {
            log.warn("Cannot dispatch WhatsApp message: phone number is empty.");
            return false;
        }

        // 1. Try Twilio if configured
        if (isTwilioConfigured() && (!"callmebot".equalsIgnoreCase(provider))) {
            return sendViaTwilio(cleanPhone, messageText);
        }

        // 2. Try CallMeBot if configured
        if (isCallMeBotConfigured() && (!"twilio".equalsIgnoreCase(provider))) {
            return sendViaCallMeBot(cleanPhone, messageText);
        }

        // 3. Fallback: Log simulation and generate direct Click-to-Chat URL
        logSimulatedMessage(cleanPhone, recipientName != null ? recipientName : "Student", title, messageText);
        return true;
    }

    /**
     * Send message via CallMeBot Free WhatsApp API
     */
    private boolean sendViaCallMeBot(String cleanPhone, String text) {
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = String.format("https://api.callmebot.com/whatsapp.php?phone=+%s&text=%s&apikey=%s",
                    cleanPhone, encodedText, callMeBotApiKey.trim());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("✅ WhatsApp alert successfully delivered to +{} via CallMeBot", cleanPhone);
                return true;
            } else {
                log.warn("⚠️ CallMeBot WhatsApp delivery returned status {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to dispatch WhatsApp via CallMeBot: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Send message via Twilio WhatsApp API
     */
    private boolean sendViaTwilio(String cleanPhone, String text) {
        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", twilioAccountSid.trim());
            String toWhatsApp = "whatsapp:+" + cleanPhone;

            String form = "From=" + URLEncoder.encode(twilioFromNumber.trim(), StandardCharsets.UTF_8)
                    + "&To=" + URLEncoder.encode(toWhatsApp, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

            String auth = Base64.getEncoder().encodeToString(
                    (twilioAccountSid.trim() + ":" + twilioAuthToken.trim()).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201 || response.statusCode() == 200) {
                log.info("✅ WhatsApp alert successfully sent to {} via Twilio", toWhatsApp);
                return true;
            } else {
                log.warn("⚠️ Twilio WhatsApp API returned status {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to dispatch WhatsApp via Twilio: {}", e.getMessage());
            return false;
        }
    }

    private void logSimulatedMessage(String cleanPhone, String name, String title, String text) {
        String clickToChatUrl = buildClickToChatUrl(cleanPhone, text);
        log.info("\n" +
                "============================================================\n" +
                "📱 [WHATSAPP DISPATCH] SWAYAM NPTEL Notification\n" +
                "------------------------------------------------------------\n" +
                "Recipient : {} (+{})\n" +
                "Title     : {}\n" +
                "Message   :\n{}\n" +
                "------------------------------------------------------------\n" +
                "🔗 Open in WhatsApp : {}\n" +
                "Status    : SIMULATED / READY (Set Twilio/CallMeBot keys for direct live delivery)\n" +
                "============================================================",
                name, cleanPhone, title, text, clickToChatUrl);
    }

    /**
     * Builds standard WhatsApp Web / Mobile Click-to-Chat URL
     */
    public String buildClickToChatUrl(String rawPhone, String text) {
        String cleanPhone = cleanPhoneNumber(rawPhone);
        String encodedText = URLEncoder.encode(text != null ? text : "", StandardCharsets.UTF_8);
        if (cleanPhone != null && !cleanPhone.isEmpty()) {
            return "https://wa.me/" + cleanPhone + "?text=" + encodedText;
        } else {
            return "https://api.whatsapp.com/send?text=" + encodedText;
        }
    }

    /**
     * Formats notification with emojis and WhatsApp markdown (*bold*, _italic_)
     */
    public String formatWhatsAppNotification(String title, String body, String studentName) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔔 *SWAYAM NPTEL ALERT*\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        if (studentName != null && !studentName.isBlank()) {
            sb.append("👋 Hello *").append(studentName.trim()).append("*,\n\n");
        }
        if (title != null && !title.isBlank()) {
            sb.append("📌 *").append(title.trim()).append("*\n\n");
        }
        if (body != null && !body.isBlank()) {
            sb.append(body.trim()).append("\n\n");
        }
        sb.append("🌐 *Portal Link:* ").append(baseUrl).append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("_SWAYAM NPTEL Assignment Manager_");
        return sb.toString();
    }

    /**
     * Formats an assignment reminder for direct WhatsApp share
     */
    public String formatAssignmentShare(Assignment assignment) {
        StringBuilder sb = new StringBuilder();
        sb.append("📚 *NPTEL Assignment Reminder*\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        if (assignment.getCourse() != null) {
            sb.append("📖 *Course:* ").append(assignment.getCourse().getCourseName()).append("\n");
            sb.append("🏷️ *Code:* ").append(assignment.getCourse().getCourseCode()).append("\n");
        }
        sb.append("📝 *Assignment:* ").append(assignment.getTitle()).append("\n");
        if (assignment.getDueDate() != null) {
            sb.append("⏰ *Due Date:* ").append(assignment.getDueDate().toString().replace("T", " ")).append("\n");
        }
        if (assignment.getSubmissionUrl() != null && !assignment.getSubmissionUrl().isBlank()) {
            sb.append("🔗 *Submit Here:* ").append(assignment.getSubmissionUrl()).append("\n");
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("_Keep tracking your SWAYAM deadlines!_");
        return sb.toString();
    }

    /**
     * Formats an exam schedule for direct WhatsApp share
     */
    public String formatExamShare(Examination exam) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎓 *NPTEL Certification Exam Alert*\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        if (exam.getCourse() != null) {
            sb.append("📖 *Course:* ").append(exam.getCourse().getCourseName()).append("\n");
        }
        if (exam.getExamDate() != null) {
            sb.append("📅 *Exam Date:* ").append(exam.getExamDate()).append("\n");
        }
        if (exam.getExamStartTime() != null && exam.getExamEndTime() != null) {
            sb.append("🕒 *Timings:* ").append(exam.getExamStartTime()).append(" - ").append(exam.getExamEndTime()).append("\n");
        }
        if (exam.getExamCenterName() != null) {
            sb.append("📍 *Center:* ").append(exam.getExamCenterName());
            if (exam.getCity() != null) sb.append(", ").append(exam.getCity());
            sb.append("\n");
        }
        if (exam.getRegistrationFee() != null) {
            sb.append("💳 *Exam Fee:* ₹").append(exam.getRegistrationFee()).append("\n");
        }
        if (exam.getRegistrationCloseDate() != null) {
            sb.append("⚠️ *Reg. Deadline:* ").append(exam.getRegistrationCloseDate()).append("\n");
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🌐 *Portal:* ").append(baseUrl).append("/examinations\n");
        sb.append("_SWAYAM NPTEL Assignment Manager_");
        return sb.toString();
    }

    /**
     * Clean phone number: strips non-digits, prepends 91 for 10-digit Indian numbers.
     */
    public String cleanPhoneNumber(String phone) {
        if (phone == null) return "";
        // Strip everything except digits
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            // Standard Indian 10-digit mobile number
            return "91" + digits;
        } else if (digits.length() == 12 && digits.startsWith("91")) {
            return digits;
        } else if (digits.startsWith("0") && digits.length() == 11) {
            return "91" + digits.substring(1);
        }
        return digits;
    }

    public boolean isTwilioConfigured() {
        return twilioAccountSid != null && !twilioAccountSid.isBlank()
                && twilioAuthToken != null && !twilioAuthToken.isBlank();
    }

    public boolean isCallMeBotConfigured() {
        return callMeBotApiKey != null && !callMeBotApiKey.isBlank();
    }
}
