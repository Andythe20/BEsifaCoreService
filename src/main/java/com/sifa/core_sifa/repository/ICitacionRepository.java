package com.sifa.core_sifa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sifa.core_sifa.model.Citacion;

@Repository
public interface ICitacionRepository extends JpaRepository<Citacion, Integer> {

    /**
     * Búsqueda paginada de citaciones con filtros opcionales.
     * Permite filtrar por rango de fecha de citación y búsqueda por
     * patente/RUT/nombre.
     */
    @Query("""
            SELECT c FROM Citacion c
            JOIN FETCH c.infraccion i
            LEFT JOIN i.vehiculo v
            LEFT JOIN v.propietarioVehiculo p
            WHERE (:startDate IS NULL OR c.fecha >= :startDate)
              AND (:endDate IS NULL OR c.fecha <= :endDate)
              AND (:search IS NULL
                   OR LOWER(v.patente) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(p.rut) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(CONCAT(p.nombres, ' ', p.apellidos)) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR CAST(i.idInfraccion AS string) LIKE CONCAT('%', :search, '%')
              )
            ORDER BY c.fecha ASC
            """)
    Page<Citacion> findByFilters(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("search") String search,
            Pageable pageable);
}
