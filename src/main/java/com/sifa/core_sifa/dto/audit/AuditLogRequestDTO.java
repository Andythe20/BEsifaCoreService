package com.sifa.core_sifa.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequestDTO {
    private String emailUsuario;
    private String accion;
    private String tablaAfectada;
    private String idRegistroAfectado;
    private Map<String, Object> detalles;
}
