package com.example.core_sifa.dto;

import com.example.core_sifa.model.AuditLog;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private String email_usuario;
    private String accion;
    private String tabla_afectada;
    private Integer id_registro_afectado;
    private Map<String, Object> detalles;
    private LocalDateTime fecha_hora;

    public static AuditLogDTO fromEntity(AuditLog auditLog){
        return AuditLogDTO.builder()
                .email_usuario(auditLog.getEmailUsuario())
                .accion(auditLog.getAccion())
                .tabla_afectada(auditLog.getTablaAfectada())
                .id_registro_afectado(auditLog.getIdRegistroAfectado())
                .detalles(auditLog.getDetalles())
                .fecha_hora(auditLog.getFechaHora())
                .build();
    }
}
