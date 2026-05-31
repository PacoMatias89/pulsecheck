package com.pulsecheck.repository;

import com.pulsecheck.domain.entity.Incident;
import com.pulsecheck.domain.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByWorkspaceIdOrderByStartedAtDesc(UUID workspaceId);

    List<Incident> findByMonitorIdOrderByStartedAtDesc(UUID monitorId);

    Optional<Incident> findByMonitorIdAndStatus(UUID monitorId, IncidentStatus status);

    @Query("""
        SELECT COUNT(i) FROM Incident i
        WHERE i.workspace.id = :workspaceId
        AND i.status = 'OPEN'
        """)
    long countOpenByWorkspace(@Param("workspaceId") UUID workspaceId);

    boolean existsByMonitorIdAndStatus(UUID monitorId, IncidentStatus status);
}
