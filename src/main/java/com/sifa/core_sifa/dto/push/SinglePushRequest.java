package com.sifa.core_sifa.dto.push;

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
public class SinglePushRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "El título es obligatorio")
    private String title;

    @NotBlank(message = "El mensaje es obligatorio")
    private String body;
}
