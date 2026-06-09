package com.sifa.core_sifa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "DEVICE_TOKENS")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_usuario", nullable = false, length = 255)
    private String emailUsuario;

    @Column(nullable = false, length = 500)
    private String token;

    @Column(nullable = false, length = 20)
    private String platform;

    @Column(name = "app_version", nullable = false, length = 20)
    private String appVersion;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "UNKNOWN";

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
