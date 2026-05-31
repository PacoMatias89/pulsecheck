package com.pulsecheck.repository;

import com.pulsecheck.domain.entity.StatusPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatusPageRepository extends JpaRepository<StatusPage, UUID> {

    List<StatusPage> findByWorkspaceId(UUID workspaceId);

    Optional<StatusPage> findBySlug(String slug);

    Optional<StatusPage> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    int countByWorkspaceId(UUID workspaceId);

    boolean existsBySlug(String slug);
}
