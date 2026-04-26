package com.example.core_sifa.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "VEHICULOS")
@Data @Builder
@AllArgsConstructor
@NoArgsConstructor
public class Vehiculo {
    
    @Id
    @NotBlank
    private String patente;

    @NotBlank
    @Column(nullable = false)
    private String marca;

    @NotBlank
    @Column(nullable = false)
    private String modelo;

    @NotNull
    @Column(nullable = false)
    private Integer anioFabricacion;

    @NotBlank
    @Column(nullable = false)
    private String color;

    @NotBlank
    @Column(nullable = false)
    private String nroMotor;

    @NotBlank
    @Column(nullable = false)
    private String nroSerie;

    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Infraccion> infracciones;

    @ManyToOne
    @JoinColumn(name = "id_propietario_vehiculo")
    private PropietarioVehiculo propietarioVehiculo;

}
