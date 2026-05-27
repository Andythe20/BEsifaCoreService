package com.sifa.core_sifa.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

// Entidad para saber la actividad del fiscalizador
@Entity
@Table(name = "INSPECTORES_PRESENCIA")
@Data @Builder
@AllArgsConstructor
@NoArgsConstructor
public class FiscalizadorPresencia {

    @Id
    private String emailUsuario;

    @Column(nullable = false)
    private Float latitud;

    @Column(nullable = false)
    private Float longitud;

    @Column(nullable = false)
    private LocalDateTime ultimaConexion;
}