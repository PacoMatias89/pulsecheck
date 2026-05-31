package com.pulsecheck.service;

import com.pulsecheck.domain.entity.User;
import com.pulsecheck.domain.entity.Workspace;
import com.pulsecheck.exception.ResourceNotFoundException;
import com.pulsecheck.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public List<Workspace> getUserWorkspaces(UUID userId) {
        return workspaceRepository.findByOwnerId(userId);
    }

    public Workspace getDefaultWorkspace(User user) {
        return workspaceRepository.findByOwnerId(user.getId())
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No workspace found for user"));
    }

    public Workspace getWorkspaceForUser(UUID workspaceId, UUID userId) {
        var workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        if (!workspace.getOwner().getId().equals(userId)) {
            throw new ResourceNotFoundException("Workspace not found");
        }
        return workspace;
    }
}
