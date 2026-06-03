package com.sifa.core_sifa.dto.audit;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

import com.sifa.core_sifa.model.AuditLog;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponseDTO {
    private String email_usuario;
    private String accion;
    private String tabla_afectada;
    private String id_registro_afectado;
    private Map<String, Object> detalles;
    private LocalDateTime fecha_hora;

    public static AuditLogResponseDTO fromEntity(AuditLog auditLog) {
        return AuditLogResponseDTO.builder()
                .email_usuario(auditLog.getEmailUsuario())
                .accion(auditLog.getAccion())
                .tabla_afectada(auditLog.getTablaAfectada())
                .id_registro_afectado(auditLog.getIdRegistroAfectado())
                .detalles(auditLog.getDetalles())
                .fecha_hora(auditLog.getFechaHora())
                .build();
    }
}
