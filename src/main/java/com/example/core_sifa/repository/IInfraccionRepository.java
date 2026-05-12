package com.example.core_sifa.repository;

import com.example.core_sifa.dto.infraccion.InfraccionResponse;
import com.example.core_sifa.model.Infraccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface IInfraccionRepository extends JpaRepository<Infraccion, Integer> {

    // Para buscar todas las infracciones que hizo un usuario en terreno
    List<Infraccion> findByIdFiscalizador(String idFiscalizador);

    // Para ver el historial de multas de un vehículo navegando por la relación
    List<Infraccion> findByVehiculoPatente(String vehiculoPatente);

    List<Infraccion> findByFechaBetween(LocalDateTime start, LocalDateTime end);
}
