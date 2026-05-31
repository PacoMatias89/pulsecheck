package com.pulsecheck.repository;

import com.pulsecheck.domain.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, UUID> {

    List<NotificationChannel> findByWorkspaceId(UUID workspaceId);

    Optional<NotificationChannel> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    List<NotificationChannel> findByWorkspaceIdAndActiveTrue(UUID workspaceId);
}
