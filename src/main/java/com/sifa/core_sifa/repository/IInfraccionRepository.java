package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.dto.infraccion.ProductividadFiscalizadorDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sifa.core_sifa.dto.infraccion.CoordenadaDTO;
import com.sifa.core_sifa.dto.infraccion.TopInfraccionDTO;
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

    @Query("""
                SELECT new com.sifa.core_sifa.dto.infraccion.CoordenadaDTO(i.latitud, i.longitud)
                FROM Infraccion i
                WHERE (:start IS NULL OR i.fecha >= :start)
                AND (:end IS NULL OR i.fecha <= :end)
                AND (:user IS NULL OR i.idFiscalizador = :user)
            """)
    List<CoordenadaDTO> findCoordenadasByFilters(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("user") String user);

    @Query("""
                SELECT new com.sifa.core_sifa.dto.infraccion.TopInfraccionDTO(i.tipoInfraccion.nombre, COUNT(i))
                FROM Infraccion i
                WHERE (:start IS NULL OR i.fecha >= :start)
                AND (:end IS NULL OR i.fecha <= :end)
                AND (:user IS NULL OR i.idFiscalizador = :user)
                GROUP BY i.tipoInfraccion.nombre
                ORDER BY COUNT(i) DESC
            """)
    List<TopInfraccionDTO> findTopInfraccionesByFilters(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("user") String user,
            Pageable pageable);

    @Query("""
                SELECT i.estado, COUNT(i)
                FROM Infraccion i
                WHERE (:start IS NULL OR i.fecha >= :start)
                AND (:end IS NULL OR i.fecha <= :end)
                AND (:user IS NULL OR i.idFiscalizador = :user)
                GROUP BY i.estado
            """)
    List<Object[]> countEstadosByFilters(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("user") String user);

    @Query("SELECT new com.sifa.core_sifa.dto.infraccion.ProductividadFiscalizadorDTO(i.idFiscalizador, COUNT(i)) " +
            "FROM Infraccion i " +
            "WHERE i.fecha BETWEEN :start AND :end " +
            "GROUP BY i.idFiscalizador " +
            "ORDER BY COUNT(i) DESC")
    List<ProductividadFiscalizadorDTO> countProductividadPorFiscalizador(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
