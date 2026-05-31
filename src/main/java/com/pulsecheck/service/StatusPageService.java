package com.pulsecheck.service;

import com.pulsecheck.domain.entity.StatusPage;
import com.pulsecheck.domain.entity.StatusPageMonitor;
import com.pulsecheck.domain.entity.User;
import com.pulsecheck.dto.request.AddMonitorToPageRequest;
import com.pulsecheck.dto.request.CreateStatusPageRequest;
import com.pulsecheck.dto.response.StatusPageResponse;
import com.pulsecheck.exception.BusinessException;
import com.pulsecheck.exception.ResourceNotFoundException;
import com.pulsecheck.repository.MonitorRepository;
import com.pulsecheck.repository.StatusPageMonitorRepository;
import com.pulsecheck.repository.StatusPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatusPageService {

    private final StatusPageRepository statusPageRepository;
    private final StatusPageMonitorRepository spmRepository;
    private final MonitorRepository monitorRepository;
    private final WorkspaceService workspaceService;

    public List<StatusPage> getStatusPages(User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        return statusPageRepository.findByWorkspaceId(workspace.getId());
    }

    public StatusPageResponse getStatusPage(UUID pageId, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        var page = statusPageRepository.findByIdAndWorkspaceId(pageId, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found"));
        return buildResponse(page);
    }

    public StatusPageResponse getPublicStatusPage(String slug) {
        var page = statusPageRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found"));
        if (!page.isPublicPage()) {
            throw new ResourceNotFoundException("Status page not found");
        }
        return buildResponse(page);
    }

    @Transactional
    public StatusPage createStatusPage(CreateStatusPageRequest req, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);

        int count = statusPageRepository.countByWorkspaceId(workspace.getId());
        if (count >= user.getPlan().maxStatusPages) {
            throw new BusinessException(
                    "Status page limit reached for your plan. Please upgrade.");
        }
        if (statusPageRepository.existsBySlug(req.slug())) {
            throw new BusinessException("Slug '" + req.slug() + "' is already taken");
        }

        var page = StatusPage.builder()
                .workspace(workspace)
                .name(req.name())
                .slug(req.slug())
                .description(req.description())
                .publicPage(req.isPublic() == null || req.isPublic())
                .primaryColor(req.primaryColor() != null ? req.primaryColor() : "#6366f1")
                .logoUrl(req.logoUrl())
                .build();

        return statusPageRepository.save(page);
    }

    @Transactional
    public void deleteStatusPage(UUID pageId, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        var page = statusPageRepository.findByIdAndWorkspaceId(pageId, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found"));
        statusPageRepository.delete(page);
    }

    @Transactional
    public void addMonitorToPage(UUID pageId, AddMonitorToPageRequest req, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        var page = statusPageRepository.findByIdAndWorkspaceId(pageId, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found"));
        var monitor = monitorRepository.findByIdAndWorkspaceId(req.monitorId(), workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));

        if (spmRepository.existsByStatusPageIdAndMonitorId(page.getId(), monitor.getId())) {
            throw new BusinessException("Monitor already on this status page");
        }

        var spm = StatusPageMonitor.builder()
                .statusPage(page)
                .monitor(monitor)
                .displayName(req.displayName() != null ? req.displayName() : monitor.getName())
                .orderIndex(req.orderIndex() != null ? req.orderIndex() : 0)
                .build();
        spmRepository.save(spm);
    }

    @Transactional
    public void removeMonitorFromPage(UUID pageId, UUID monitorId, User user) {
        var workspace = workspaceService.getDefaultWorkspace(user);
        statusPageRepository.findByIdAndWorkspaceId(pageId, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found"));
        spmRepository.deleteByStatusPageIdAndMonitorId(pageId, monitorId);
    }

    private StatusPageResponse buildResponse(StatusPage page) {
        var monitors = spmRepository.findByStatusPageIdOrderByOrderIndex(page.getId())
                .stream()
                .map(spm -> new StatusPageResponse.StatusPageMonitorResponse(
                        spm.getId(),
                        spm.getMonitor().getId(),
                        spm.getDisplayName(),
                        spm.getMonitor().getStatus().name(),
                        spm.getOrderIndex()
                ))
                .toList();

        return new StatusPageResponse(
                page.getId(), page.getName(), page.getSlug(), page.getCustomDomain(),
                page.getDescription(), page.isPublicPage(), page.getLogoUrl(),
                page.getPrimaryColor(), page.getCreatedAt(), monitors
        );
    }
}
