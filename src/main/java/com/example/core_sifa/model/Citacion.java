package com.example.core_sifa.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CITACIONES")
@Data @Builder
@AllArgsConstructor
@NoArgsConstructor
public class Citacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCitacion;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = true)
    private Boolean listadoCorte;

    @Column(nullable = true)
    private LocalDateTime fechaFallo;

    @Column(nullable = true)
    private LocalDateTime fechaArchivo;

    @OneToOne
    @JoinColumn(name = "idInfraccion", nullable = false, unique = true)
    private Infraccion infraccion;
}
