package com.sifa.core_sifa.model;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AUDIT_LOGS")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAuditLog;

    @NotNull
    @Column(nullable = false)
    private String emailUsuario;

    @NotBlank
    @Column(nullable = false)
    private String accion;

    @Column(nullable = true)
    private String tablaAfectada;

    @Column(nullable = true)
    private String idRegistroAfectado;

    // No existe un dato natiivo JSON, se usa el par clave valor para flexibilidad
    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON) // traductor necesario para el traspaso del dato entre la bd y java
    @NotNull
    @Column(columnDefinition = "json", nullable = false)
    private Map<String, Object> detalles;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fechaHora;

    // Autogenerar la fecha antes de insertar para no depender del controlador
    @PrePersist
    protected void onCreate() {
        this.fechaHora = LocalDateTime.now();
    }

}
