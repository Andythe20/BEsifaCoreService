package com.example.core_sifa.service;

import com.example.core_sifa.dto.TipoInfraccionDTO;
import com.example.core_sifa.exception.ResourceNotFoundException;
import com.example.core_sifa.model.TipoInfraccion;
import com.example.core_sifa.repository.ITipoInfraccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TipoInfraccionService {

    private final ITipoInfraccionRepository tipoInfraccionRepository;

    @Transactional(readOnly = true)
    public List<TipoInfraccionDTO> findAll(){
        log.info("Listando todos los tipos de infracción");

        return tipoInfraccionRepository.findAll()
                .stream()
                .map(TipoInfraccionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TipoInfraccionDTO findById(Integer id){
        log.info("Buscando tipo de infracción con id: {}", id);
        TipoInfraccion tipoInfraccion = tipoInfraccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de infracción no encontrado o inexistente"));

        return TipoInfraccionDTO.fromEntity(tipoInfraccion);
    }
}
