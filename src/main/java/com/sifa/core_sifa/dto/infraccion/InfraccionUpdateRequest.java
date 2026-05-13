package com.sifa.core_sifa.dto.infraccion;

import lombok.*;

import java.time.LocalDateTime;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InfraccionUpdateRequest {

    private String estado;
    private String motivoRechazo;

}
