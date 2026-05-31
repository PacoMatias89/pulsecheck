package com.pulsecheck.repository;

import com.pulsecheck.domain.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    List<Workspace> findByOwnerId(UUID ownerId);
    Optional<Workspace> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
