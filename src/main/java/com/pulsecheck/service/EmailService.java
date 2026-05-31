package com.pulsecheck.service;

import com.pulsecheck.domain.entity.Monitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async("monitorExecutor")
    public void send(String to, String subject, String htmlBody) {
        try {
            var msg = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
            log.debug("Email sent to {} | subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public String buildDownAlert(Monitor monitor) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
                  <div style="background:#ef4444;color:white;padding:20px;border-radius:8px 8px 0 0;">
                    <h1 style="margin:0;font-size:24px;">🔴 Service Down Alert</h1>
                  </div>
                  <div style="background:#f9f9f9;padding:20px;border:1px solid #ddd;border-radius:0 0 8px 8px;">
                    <p style="font-size:18px;font-weight:bold;color:#111;">%s is DOWN</p>
                    <p><strong>URL:</strong> <a href="%s">%s</a></p>
                    <p><strong>Time:</strong> %s UTC</p>
                    <br>
                    <a href="%s/dashboard" style="background:#6366f1;color:white;padding:12px 24px;
                       border-radius:6px;text-decoration:none;">View Dashboard</a>
                  </div>
                  <p style="color:#888;font-size:12px;margin-top:16px;">
                    You're receiving this because you set up monitoring on PulseCheck.
                    <a href="%s/settings/notifications">Manage notifications</a>
                  </p>
                </body>
                </html>
                """.formatted(
                monitor.getName(), monitor.getUrl(), monitor.getUrl(),
                DateTimeFormatter.ISO_INSTANT.format(
                        java.time.Instant.now().atZone(ZoneOffset.UTC)),
                baseUrl, baseUrl
        );
    }

    public String buildRecoveryEmail(Monitor monitor) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;">
                  <div style="background:#22c55e;color:white;padding:20px;border-radius:8px 8px 0 0;">
                    <h1 style="margin:0;font-size:24px;">✅ Service Recovered</h1>
                  </div>
                  <div style="background:#f9f9f9;padding:20px;border:1px solid #ddd;border-radius:0 0 8px 8px;">
                    <p style="font-size:18px;font-weight:bold;color:#111;">%s is back UP</p>
                    <p><strong>URL:</strong> <a href="%s">%s</a></p>
                    <p><strong>Recovered at:</strong> %s UTC</p>
                    <br>
                    <a href="%s/dashboard" style="background:#6366f1;color:white;padding:12px 24px;
                       border-radius:6px;text-decoration:none;">View Dashboard</a>
                  </div>
                </body>
                </html>
                """.formatted(
                monitor.getName(), monitor.getUrl(), monitor.getUrl(),
                DateTimeFormatter.ISO_INSTANT.format(
                        java.time.Instant.now().atZone(ZoneOffset.UTC)),
                baseUrl
        );
    }
}
