package com.pulsecheck.service;

import com.pulsecheck.domain.entity.*;
import com.pulsecheck.domain.enums.NotificationChannelType;
import com.pulsecheck.dto.request.CreateNotificationChannelRequest;
import com.pulsecheck.dto.request.CreateNotificationRuleRequest;
import com.pulsecheck.exception.BusinessException;
import com.pulsecheck.exception.ResourceNotFoundException;
import com.pulsecheck.repository.NotificationChannelRepository;
import com.pulsecheck.repository.NotificationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationChannelRepository channelRepository;
    private final NotificationRuleRepository ruleRepository;
    private final WorkspaceService workspaceService;
    private final EmailService emailService;

    public List<NotificationChannel> getChannels(User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        return channelRepository.findByWorkspaceId(workspace.getId());
    }

    @Transactional
    public NotificationChannel createChannel(CreateNotificationChannelRequest req, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        var channel = NotificationChannel.builder()
                .workspace(workspace)
                .name(req.name())
                .type(req.type())
                .config(req.config())
                .build();
        return channelRepository.save(channel);
    }

    @Transactional
    public void deleteChannel(UUID channelId, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        var channel = channelRepository.findByIdAndWorkspaceId(channelId, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found"));
        channelRepository.delete(channel);
    }

    @Transactional
    public NotificationRule createRule(UUID monitorId, CreateNotificationRuleRequest req, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        var channel = channelRepository.findByIdAndWorkspaceId(req.channelId(), workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found"));

        if (ruleRepository.existsByMonitorIdAndChannelId(monitorId, channel.getId())) {
            throw new BusinessException("Rule already exists for this monitor and channel");
        }

        var monitor = new Monitor();
        monitor.setId(monitorId);

        var rule = NotificationRule.builder()
                .monitor(monitor)
                .channel(channel)
                .notifyOnDown(req.notifyOnDown() == null || req.notifyOnDown())
                .notifyOnRecovery(req.notifyOnRecovery() == null || req.notifyOnRecovery())
                .build();
        return ruleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(UUID monitorId, UUID channelId) {
        ruleRepository.deleteByMonitorIdAndChannelId(monitorId, channelId);
    }

    @Async("monitorExecutor")
    public void sendDownAlert(Monitor monitor, Incident incident) {
        var rules = ruleRepository.findActiveDownRulesForMonitor(monitor.getId());
        rules.forEach(rule -> sendNotification(rule.getChannel(), monitor, incident, false));
    }

    @Async("monitorExecutor")
    public void sendRecoveryAlert(Monitor monitor) {
        var rules = ruleRepository.findActiveRecoveryRulesForMonitor(monitor.getId());
        rules.forEach(rule -> sendNotification(rule.getChannel(), monitor, null, true));
    }

    private void sendNotification(NotificationChannel channel, Monitor monitor,
                                   Incident incident, boolean isRecovery) {
        try {
            switch (channel.getType()) {
                case EMAIL   -> sendEmail(channel, monitor, isRecovery);
                case SLACK   -> sendWebhook(channel, buildSlackPayload(monitor, isRecovery));
                case DISCORD -> sendWebhook(channel, buildDiscordPayload(monitor, isRecovery));
                case WEBHOOK -> sendWebhook(channel, buildGenericPayload(monitor, isRecovery));
                case TELEGRAM -> sendTelegram(channel, monitor, isRecovery);
            }
        } catch (Exception e) {
            log.error("Failed to send notification via {} channel {}: {}",
                    channel.getType(), channel.getId(), e.getMessage());
        }
    }

    private void sendEmail(NotificationChannel channel, Monitor monitor, boolean isRecovery) {
        var to = channel.getConfig().get("email");
        if (to == null) return;
        var subject = isRecovery
                ? "[RECOVERED] " + monitor.getName() + " is back up"
                : "[ALERT] " + monitor.getName() + " is DOWN";
        var body = isRecovery
                ? emailService.buildRecoveryEmail(monitor)
                : emailService.buildDownAlert(monitor);
        emailService.send(to, subject, body);
    }

    private void sendWebhook(NotificationChannel channel, String payload) {
        var url = channel.getConfig().get("webhookUrl");
        if (url == null) return;
        try {
            var client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Webhook delivery failed to {}: {}", url, e.getMessage());
        }
    }

    private void sendTelegram(NotificationChannel channel, Monitor monitor, boolean isRecovery) {
        var botToken = channel.getConfig().get("botToken");
        var chatId   = channel.getConfig().get("chatId");
        if (botToken == null || chatId == null) return;
        var emoji = isRecovery ? "✅" : "🔴";
        var text  = isRecovery
                ? emoji + " *" + monitor.getName() + "* is back UP"
                : emoji + " *" + monitor.getName() + "* is DOWN\\n" + monitor.getUrl();
        var url  = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        var body = "{\"chat_id\":\"" + chatId + "\",\"text\":\"" + text + "\",\"parse_mode\":\"Markdown\"}";
        sendWebhookRaw(url, body);
    }

    private String buildSlackPayload(Monitor monitor, boolean isRecovery) {
        var color = isRecovery ? "#36a64f" : "#ff0000";
        var title = isRecovery ? monitor.getName() + " is back UP" : monitor.getName() + " is DOWN";
        return "{\"attachments\":[{\"color\":\"" + color + "\",\"title\":\"" + title
                + "\",\"text\":\"" + monitor.getUrl() + "\"}]}";
    }

    private String buildDiscordPayload(Monitor monitor, boolean isRecovery) {
        var color = isRecovery ? 3066993 : 15158332;
        var title = isRecovery ? monitor.getName() + " is back UP" : monitor.getName() + " is DOWN";
        return "{\"embeds\":[{\"title\":\"" + title + "\",\"color\":" + color
                + ",\"description\":\"" + monitor.getUrl() + "\"}]}";
    }

    private String buildGenericPayload(Monitor monitor, boolean isRecovery) {
        return "{\"monitor\":\"" + monitor.getName()
                + "\",\"url\":\"" + monitor.getUrl()
                + "\",\"status\":\"" + (isRecovery ? "UP" : "DOWN") + "\"}";
    }

    private void sendWebhookRaw(String url, String body) {
        try {
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Raw webhook failed: {}", e.getMessage());
        }
    }
}
