package com.sifa.core_sifa.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sifa.core_sifa.dto.TipoInfraccionDTO;
import com.sifa.core_sifa.service.TipoInfraccionServiceImpl;

@RestController
@RequestMapping("/core/api/v1/tipoInfracciones")
@RequiredArgsConstructor
@Slf4j
public class TipoInfraccionController {

    private final TipoInfraccionServiceImpl tipoInfraccionService;

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_SUPERVISOR', 'USER_APP')")
    @GetMapping("/all")
    public ResponseEntity<Page<TipoInfraccionDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Obteniendo tipos de infracción paginados - página: {}, tamaño: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(tipoInfraccionService.findAllPaged(pageable));
    }

    @PreAuthorize("hasAnyAuthority('USER_ADMIN', 'USER_JPL', 'USER_APP')")
    @GetMapping("/id/{id}")
    public ResponseEntity<TipoInfraccionDTO> findById(@PathVariable Integer id) {
        TipoInfraccionDTO tipoInfraccion = tipoInfraccionService.findById(id);

        return ResponseEntity.ok(tipoInfraccion);
    }

    // CREATE
    @PreAuthorize("hasAnyAuthority('USER_ADMIN')")
    @PostMapping
    public ResponseEntity<TipoInfraccionDTO> create(
            @RequestBody TipoInfraccionDTO tipoInfraccionDTO) {

        log.info("Creando tipo de infracción");

        TipoInfraccionDTO created =
                tipoInfraccionService.create(tipoInfraccionDTO);

        return ResponseEntity.ok(created);
    }

    // UPDATE
    @PreAuthorize("hasAnyAuthority('USER_ADMIN')")
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
