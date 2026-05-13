package com.sifa.core_sifa.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AUDIT_LOGS")
@Data @Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAuditLog;

    @NotNull
    @Column(nullable = false)
    private String emailUsuario;

    @NotBlank
    @Column(nullable = false)
    private String accion;

    @NotBlank
    @Column(nullable = false)
    private String tablaAfectada;

    @NotNull
    @Column(nullable = false)
    private Integer idRegistroAfectado;

    // No existe un dato natiivo JSON, se usa el par clave valor para flexibilidad
    @JdbcTypeCode(org.hibernate.type.SqlTypes.JSON) // traductor necesario para el traspaso del dato entre la bd y java
    @NotNull
    @Column(columnDefinition = "json", nullable = false)
    private Map<String, Object> detalles;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fechaHora;

}
