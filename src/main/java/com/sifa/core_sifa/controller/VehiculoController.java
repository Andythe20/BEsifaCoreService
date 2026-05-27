package com.sifa.core_sifa.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sifa.core_sifa.dto.VehiculoDTO;
import com.sifa.core_sifa.service.VehiculoService;

import java.util.List;

/**
 * La tabla Vehiculos es parte de la api simulada para consultar datos reales, por ende,
 * las funciones implementadas son solo para lectura, nada más.
 */
@Hidden
// <-- no queremos mostrar esto en la documentacion ya que pertenece a la simulacion de la api del registro civil
@RestController
@RequestMapping("/core/api/v1/vehiculos")
@RequiredArgsConstructor
@Slf4j
public class VehiculoController {

    private final VehiculoService vehiculoService;

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_APP')")
    @GetMapping("/all")
    public ResponseEntity<List<VehiculoDTO>> findAllVehiculos() {
        log.info("Obteniendo todos los vehículos");
        List<VehiculoDTO> vehiculos = vehiculoService.findAllVehiculos();

        if (vehiculos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(vehiculos);

    }

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_APP')")
    @GetMapping("/id/{id}")
    public ResponseEntity<VehiculoDTO> findById(@PathVariable String id) {
        log.info("Obteniendo vehiculo con id: {}", id);
        VehiculoDTO vehiculo = vehiculoService.findById(id);
        return ResponseEntity.ok(vehiculo);
    }

}
