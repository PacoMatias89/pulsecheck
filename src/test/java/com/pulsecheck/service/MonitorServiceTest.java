package com.pulsecheck.service;

import com.pulsecheck.domain.entity.Monitor;
import com.pulsecheck.domain.entity.User;
import com.pulsecheck.domain.entity.Workspace;
import com.pulsecheck.domain.enums.MonitorStatus;
import com.pulsecheck.domain.enums.MonitorType;
import com.pulsecheck.domain.enums.UserPlan;
import com.pulsecheck.dto.request.CreateMonitorRequest;
import com.pulsecheck.exception.BusinessException;
import com.pulsecheck.repository.MonitorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock MonitorRepository monitorRepository;
    @Mock WorkspaceService workspaceService;

    @InjectMocks MonitorService monitorService;

    private User user;
    private Workspace workspace;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .plan(UserPlan.FREE)
                .build();

        workspace = Workspace.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .name("Test Workspace")
                .slug("test")
                .build();

        when(workspaceService.getDefaultWorkspace(user)).thenReturn(workspace);
    }

    @Test
    void createMonitor_withinPlanLimit_succeeds() {
        when(monitorRepository.countByWorkspaceId(workspace.getId())).thenReturn(0);
        var saved = Monitor.builder()
                .id(UUID.randomUUID()).name("Test").url("https://example.com")
                .type(MonitorType.HTTPS).status(MonitorStatus.PENDING).build();
        when(monitorRepository.save(any())).thenReturn(saved);

        var req = new CreateMonitorRequest(
                "Test", "https://example.com", MonitorType.HTTPS,
                "GET", 200, null, null, 300, 30000);

        var result = monitorService.createMonitor(req, user);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test");
        verify(monitorRepository).save(any(Monitor.class));
    }

    @Test
    void createMonitor_exceedsPlanLimit_throwsBusinessException() {
        when(monitorRepository.countByWorkspaceId(workspace.getId()))
                .thenReturn(UserPlan.FREE.maxMonitors);

        var req = new CreateMonitorRequest(
                "Test", "https://example.com", MonitorType.HTTPS,
                "GET", 200, null, null, 300, 30000);

        assertThatThrownBy(() -> monitorService.createMonitor(req, user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Monitor limit reached");

        verify(monitorRepository, never()).save(any());
    }

    @Test
    void createMonitor_intervalBelowPlanMin_throwsBusinessException() {
        when(monitorRepository.countByWorkspaceId(workspace.getId())).thenReturn(0);

        var req = new CreateMonitorRequest(
                "Test", "https://example.com", MonitorType.HTTPS,
                "GET", 200, null, null, 30, 30000);

        assertThatThrownBy(() -> monitorService.createMonitor(req, user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Minimum check interval");
    }

    @Test
    void createMonitor_normalizesUrlWithoutProtocol() {
        when(monitorRepository.countByWorkspaceId(workspace.getId())).thenReturn(0);
        when(monitorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var req = new CreateMonitorRequest(
                "Test", "example.com", MonitorType.HTTPS,
                "GET", 200, null, null, 300, 30000);

        var result = monitorService.createMonitor(req, user);

        assertThat(result.getUrl()).startsWith("https://");
    }
}
