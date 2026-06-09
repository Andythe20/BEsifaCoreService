package com.sifa.core_sifa.dto.device;

import com.sifa.core_sifa.model.DeviceToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DeviceTokenResponse {

    private Long id;
    private String emailUsuario;
    private String platform;
    private String appVersion;
    private String deviceModel;
    private String manufacturer;
    private LocalDateTime lastHeartbeatAt;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DeviceTokenResponse fromEntity(DeviceToken entity) {
        return DeviceTokenResponse.builder()
                .id(entity.getId())
                .emailUsuario(entity.getEmailUsuario())
                .platform(entity.getPlatform())
                .appVersion(entity.getAppVersion())
                .deviceModel(entity.getDeviceModel())
                .manufacturer(entity.getManufacturer())
                .lastHeartbeatAt(entity.getLastHeartbeatAt())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
