package com.spring.mcp.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing scheduler settings for automatic synchronization.
 * Controls when and how automatic comprehensive syncs are executed.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-12
 */
@Entity
@Table(name = "scheduler_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Whether automatic synchronization is enabled
     */
    @Column(name = "sync_enabled", nullable = false)
    private Boolean syncEnabled;

    /**
     * Time to run sync in HH:mm 24-hour format (e.g., "03:00")
     */
    @Column(name = "sync_time", nullable = false, length = 5)
    private String syncTime;

    /**
     * Display format preference: "12h" or "24h"
     */
    @Column(name = "time_format", nullable = false, length = 3)
    private String timeFormat;

    /**
     * Timestamp of last automatic sync execution
     */
    @Column(name = "last_sync_run")
    private LocalDateTime lastSyncRun;

    /**
     * Calculated timestamp for next scheduled sync
     */
    @Column(name = "next_sync_run")
    private LocalDateTime nextSyncRun;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
