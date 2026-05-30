package com.sifa.core_sifa.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

// Entidad para saber la actividad del fiscalizador
@Entity
@Table(name = "INSPECTORES_PRESENCIA")
@IdClass(FiscalizadorPresenciaId.class) // Vinculamos la llave compuesta
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FiscalizadorPresencia {

    @Id
    private String emailUsuario;

    @Id
    private LocalDateTime ultimaConexion;

    @Column(nullable = false)
    private Float latitud;

    @Column(nullable = false)
    private Float longitud;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "marca_dispositivo")
    private String marca;

    @Column(name = "modelo_dispositivo")
    private String modelo;

}