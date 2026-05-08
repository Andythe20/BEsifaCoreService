package com.example.core_sifa.service;

import com.example.core_sifa.dto.VehiculoDTO;
import com.example.core_sifa.exception.ResourceNotFoundException;
import com.example.core_sifa.model.Vehiculo;
import com.example.core_sifa.repository.IVehiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/** La tabla Vehiculos es parte de la api simulada para consultar datos reales, por ende,
 * las funciones implementadas son solo para lectura, nada más.*/
@Service
@Slf4j
@RequiredArgsConstructor
public class VehiculoService {

    private final IVehiculoRepository vehiculoRepository;

    @Transactional(readOnly = true)
    public List<VehiculoDTO> findAllVehiculos() {
        log.info("Listando todos los vehículos");

        return vehiculoRepository.findAll()
                .stream()
                .map(VehiculoDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehiculoDTO findById(String id) {
        log.info("Buscando vehículo con id: {}", id);
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado o inexistente"));

        return VehiculoDTO.fromEntity(vehiculo);
    }
}
