package com.sifa.core_sifa.dto.citacion;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitacionUpdateRequest {
    @Schema(description = "Nueva fecha para la citacion", example = "2026-05-26 15:19:36.828831")
    private LocalDateTime fecha;
}
