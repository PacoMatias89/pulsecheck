package com.pulsecheck.repository;

import com.pulsecheck.domain.entity.StatusPageMonitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatusPageMonitorRepository extends JpaRepository<StatusPageMonitor, UUID> {

    List<StatusPageMonitor> findByStatusPageIdOrderByOrderIndex(UUID statusPageId);

    Optional<StatusPageMonitor> findByStatusPageIdAndMonitorId(UUID statusPageId, UUID monitorId);

    void deleteByStatusPageIdAndMonitorId(UUID statusPageId, UUID monitorId);

    boolean existsByStatusPageIdAndMonitorId(UUID statusPageId, UUID monitorId);
}
