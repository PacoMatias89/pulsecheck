package com.pulsecheck.repository;

import com.pulsecheck.domain.entity.NotificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRuleRepository extends JpaRepository<NotificationRule, UUID> {

    List<NotificationRule> findByMonitorId(UUID monitorId);

    @Query("""
        SELECT nr FROM NotificationRule nr
        JOIN FETCH nr.channel c
        WHERE nr.monitor.id = :monitorId
        AND nr.notifyOnDown = true
        AND c.active = true
        """)
    List<NotificationRule> findActiveDownRulesForMonitor(@Param("monitorId") UUID monitorId);

    @Query("""
        SELECT nr FROM NotificationRule nr
        JOIN FETCH nr.channel c
        WHERE nr.monitor.id = :monitorId
        AND nr.notifyOnRecovery = true
        AND c.active = true
        """)
    List<NotificationRule> findActiveRecoveryRulesForMonitor(@Param("monitorId") UUID monitorId);

    void deleteByMonitorIdAndChannelId(UUID monitorId, UUID channelId);

    boolean existsByMonitorIdAndChannelId(UUID monitorId, UUID channelId);
}
