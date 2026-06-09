package com.sifa.core_sifa.dto.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectPushRequest {

    @NotBlank(message = "El título es obligatorio")
    private String title;

    @NotBlank(message = "El mensaje es obligatorio")
    private String body;

    @NotEmpty(message = "Debe seleccionar al menos un dispositivo")
    private List<Long> deviceIds;
}
