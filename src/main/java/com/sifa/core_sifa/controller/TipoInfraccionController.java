package com.sifa.core_sifa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sifa.core_sifa.dto.TipoInfraccionDTO;
import com.sifa.core_sifa.service.TipoInfraccionServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/core/api/v1/tipoInfracciones")
@RequiredArgsConstructor
@Slf4j
public class TipoInfraccionController {

    private final TipoInfraccionServiceImpl tipoInfraccionService;

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_APP')")
    @GetMapping("/all")
    public ResponseEntity<List<TipoInfraccionDTO>> findAll() {
        log.info("Obteniendo todos los tipos de infracción");
        List<TipoInfraccionDTO> tiposInfraccion = tipoInfraccionService.findAll();

        return ResponseEntity.ok(tiposInfraccion);
    }

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_APP')")
    @GetMapping("/id/{id}")
    public ResponseEntity<TipoInfraccionDTO> findById(@PathVariable Integer id) {
        TipoInfraccionDTO tipoInfraccion = tipoInfraccionService.findById(id);

        return ResponseEntity.ok(tipoInfraccion);
    }

    // CREATE
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL')")
    @PostMapping
    public ResponseEntity<TipoInfraccionDTO> create(
            @RequestBody TipoInfraccionDTO tipoInfraccionDTO) {

        log.info("Creando tipo de infracción");

        TipoInfraccionDTO created =
                tipoInfraccionService.create(tipoInfraccionDTO);

        return ResponseEntity.ok(created);
    }

    // UPDATE
    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL')")
    @PutMapping("/{id}")
    public ResponseEntity<TipoInfraccionDTO> update(
            @PathVariable Integer id,
            @RequestBody TipoInfraccionDTO tipoInfraccionDTO) {

        log.info("Actualizando tipo de infracción con id {}", id);

        TipoInfraccionDTO updated =
                tipoInfraccionService.update(id, tipoInfraccionDTO);

        return ResponseEntity.ok(updated);
    }

    // DELETE
    @PreAuthorize("hasAnyAuthority('USER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {

        log.info("Eliminando tipo de infracción con id {}", id);

        tipoInfraccionService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
