package com.sifa.core_sifa.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa una fotografía (evidencia) adjunta a una infracción.
 * <p>
 * Para poder demostrar que el archivo no fue sustituido, se persiste el hash
 * SHA-256 calculado al momento de la recepción junto con la versión del objeto
 * en S3 y el dispositivo que lo capturó. Esto permite re-calcular el hash al
 * consultar/exportar y detectar cualquier alteración.
 */
@Entity
@Table(name = "EVIDENCIAS_FOTOGRAFICAS")
@Data @Builder
@AllArgsConstructor
@NoArgsConstructor
public class EvidenciaFotografica {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEvidenciaFotografica;

    @NotBlank
    @Column(nullable = false)
    private String url;

    // Hash SHA-256 del archivo al momento de su recepción (detecta sustitución)
    @NotBlank
    @Column(nullable = false, length = 64)
    private String sha256Hash;

    // Versión del objeto en S3 (0 si el bucket no tiene versioning habilitado)
    @NotNull
    @Column(nullable = false)
    private Integer versionObjeto;

    // Identificador del dispositivo móvil que capturó la fotografía
    @Column(nullable = true)
    private String idDispositivo;

    // Sello de tiempo de registro de la evidencia
    @NotNull
    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "idInfraccion")
    private Infraccion infraccion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
        if (this.versionObjeto == null) {
            this.versionObjeto = 0;
        }
    }
}
