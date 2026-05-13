package com.sifa.core_sifa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @ManyToOne
    @NotNull
    @JoinColumn(name = "idInfraccion")
    private Infraccion infraccion;
}
