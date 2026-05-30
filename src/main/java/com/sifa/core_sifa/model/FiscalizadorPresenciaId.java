package com.sifa.core_sifa.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiscalizadorPresenciaId implements Serializable {
    private String emailUsuario;
    private LocalDateTime ultimaConexion;
}