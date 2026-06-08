package com.sifa.core_sifa.dto.device;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegisterRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La plataforma es obligatoria")
    private String platform;

    @NotBlank(message = "La versión de la app es obligatoria")
    private String appVersion;
}
