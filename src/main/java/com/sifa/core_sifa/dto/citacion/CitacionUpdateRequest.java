package com.sifa.core_sifa.dto.citacion;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitacionUpdateRequest {
    private LocalDateTime fecha;
}
