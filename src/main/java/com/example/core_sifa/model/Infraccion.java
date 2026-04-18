package com.example.core_sifa.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "INFRACCIONES")
@Data @Builder
@AllArgsConstructor
@NoArgsConstructor
public class Infraccion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_infraccion;

    @NotNull
    @Column(nullable = false)
    private UUID id_usuario_jpl;

    @NotNull
    @Column(nullable = false)
    private UUID id_fiscalizador;

    @Column(nullable = true)
    private String observaciones;

    @NotBlank
    @Column(nullable = false)
    private String lugar;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fecha;

    @NotBlank
    @Column(nullable = false)
    private String estado;

    @Column(nullable = true)
    private String motivo_rechazo;

    @Column(nullable = true)
    private LocalDateTime fecha_resolucion;

    @NotNull
    @Column(nullable = false)
    private Float latitud;

    @NotNull
    @Column(nullable = false)
    private Float longitud;

    @OneToMany(mappedBy = "infraccion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvidenciaFotografica> evidenciasFotograficas;

    @ManyToOne
    @JoinColumn(name = "id_tipo_infraccion")
    private TipoInfraccion tipo_infraccion;

    @OneToOne(mappedBy = "infraccion", cascade = CascadeType.ALL)
    private Citacion citacion;

    @ManyToOne
    @JoinColumn(name = "id_vehiculo")
    private Vehiculo vehiculo;

}
