package com.pulsecheck.repository;

import com.pulsecheck.domain.entity.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitorRepository extends JpaRepository<Monitor, UUID> {

    List<Monitor> findByWorkspaceId(UUID workspaceId);

    int countByWorkspaceId(UUID workspaceId);

    Optional<Monitor> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Query("""
        SELECT m FROM Monitor m
        WHERE m.active = true
        AND (m.lastCheckedAt IS NULL OR m.lastCheckedAt <= :cutoff)
        ORDER BY m.lastCheckedAt ASC NULLS FIRST
        """)
    List<Monitor> findMonitorsDue(@Param("cutoff") Instant cutoff);

    @Query("""
        SELECT m FROM Monitor m
        WHERE m.active = true
        AND m.workspace.id = :workspaceId
        AND (m.lastCheckedAt IS NULL OR m.lastCheckedAt <= :cutoff)
        """)
    List<Monitor> findDueByWorkspace(@Param("workspaceId") UUID workspaceId,
                                     @Param("cutoff") Instant cutoff);
}
