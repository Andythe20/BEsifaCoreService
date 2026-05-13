package com.sifa.core_sifa.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TIPOS_INFRACCION")
@Data @Builder
@AllArgsConstructor
@NoArgsConstructor
public class TipoInfraccion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idTipoInfraccion;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    @Column(nullable = true)
    private String disposicionInfringida;

    @OneToMany(mappedBy = "tipoInfraccion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Infraccion> infracciones;
}
