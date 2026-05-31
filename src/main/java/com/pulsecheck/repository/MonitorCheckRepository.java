package com.pulsecheck.repository;

import com.pulsecheck.domain.entity.MonitorCheck;
import com.pulsecheck.domain.enums.CheckStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MonitorCheckRepository extends JpaRepository<MonitorCheck, UUID> {

    List<MonitorCheck> findByMonitorIdOrderByCheckedAtDesc(UUID monitorId, Pageable pageable);

    @Query("""
        SELECT COUNT(c) FROM MonitorCheck c
        WHERE c.monitor.id = :monitorId
        AND c.checkedAt >= :since
        """)
    long countByMonitorSince(@Param("monitorId") UUID monitorId, @Param("since") Instant since);

    @Query("""
        SELECT COUNT(c) FROM MonitorCheck c
        WHERE c.monitor.id = :monitorId
        AND c.status = :status
        AND c.checkedAt >= :since
        """)
    long countByMonitorAndStatusSince(@Param("monitorId") UUID monitorId,
                                       @Param("status") CheckStatus status,
                                       @Param("since") Instant since);

    @Query("""
        SELECT AVG(c.responseTimeMs) FROM MonitorCheck c
        WHERE c.monitor.id = :monitorId
        AND c.checkedAt >= :since
        AND c.responseTimeMs IS NOT NULL
        """)
    Double avgResponseTimeSince(@Param("monitorId") UUID monitorId, @Param("since") Instant since);

    @Query("""
        SELECT c FROM MonitorCheck c
        WHERE c.monitor.id = :monitorId
        AND c.checkedAt >= :since
        ORDER BY c.checkedAt ASC
        """)
    List<MonitorCheck> findByMonitorSince(@Param("monitorId") UUID monitorId,
                                           @Param("since") Instant since);

    void deleteByCheckedAtBefore(Instant cutoff);
}
