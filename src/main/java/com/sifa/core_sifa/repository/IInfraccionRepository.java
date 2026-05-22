package com.sifa.core_sifa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sifa.core_sifa.model.Infraccion;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IInfraccionRepository extends JpaRepository<Infraccion, Integer> {

    // Para buscar todas las infracciones por fecha descendiente
    // Page<Infraccion> findAllByOrderByFechaDesc(Pageable pageable);

    // Para buscar todas las infracciones que hizo un usuario en terreno
    Page<Infraccion> findByIdFiscalizadorOrderByFechaDesc(String idFiscalizador, Pageable pageable);

    // Para ver el historial de multas de un vehículo navegando por la relación
    List<Infraccion> findByVehiculoPatenteOrderByFechaDesc(String vehiculoPatente);

    // Para buscar todas las infracciones que se hicieron en un rango de fechas
    List<Infraccion> findByFechaBetweenOrderByFechaDesc(LocalDateTime start, LocalDateTime end);

    // Para buscar todas las infracciones que hizo un usuario en terreno en un rango
    // de fechas
    List<Infraccion> findByFechaBetweenAndIdFiscalizadorOrderByFechaDesc(
            LocalDateTime start,
            LocalDateTime end,
            String idFiscalizador);

    @Query("""
                SELECT i
                FROM Infraccion i
                WHERE (:start IS NULL OR i.fecha >= :start)
                AND (:end IS NULL OR i.fecha <= :end)
                AND (:user IS NULL OR i.idFiscalizador = :user)
            """)
    Page<Infraccion> findByFilters(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("user") String user,
            Pageable pageable);
}
