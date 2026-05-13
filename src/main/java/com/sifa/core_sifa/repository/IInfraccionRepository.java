package com.sifa.core_sifa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sifa.core_sifa.model.Infraccion;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IInfraccionRepository extends JpaRepository<Infraccion, Integer> {

    // Para buscar todas las infracciones que hizo un usuario en terreno
    List<Infraccion> findByIdFiscalizador(String idFiscalizador);

    // Para ver el historial de multas de un vehículo navegando por la relación
    List<Infraccion> findByVehiculoPatente(String vehiculoPatente);

    // Para buscar todas las infracciones que se hicieron en un rango de fechas
    List<Infraccion> findByFechaBetween(LocalDateTime start, LocalDateTime end);

    // Para buscar todas las infracciones que hizo un usuario en terreno en un rango de fechas
    List<Infraccion> findByFechaBetweenAndIdFiscalizador(
        LocalDateTime start,
        LocalDateTime end,
        String idFiscalizador
    );
}
