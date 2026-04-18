package com.example.core_sifa.model;

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
    private Integer id_tipo_infraccion;

    @NotBlank
    @Column(nullable = false)
    private String descripcion;

    @OneToMany(mappedBy = "tipo_infraccion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Infraccion> infracciones;
}
