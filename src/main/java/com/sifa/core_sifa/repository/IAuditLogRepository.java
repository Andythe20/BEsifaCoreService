package com.sifa.core_sifa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sifa.core_sifa.model.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IAuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
             SELECT a
             FROM AuditLog a
             WHERE (:start IS NULL OR a.fechaHora >= :start)
             AND (:end IS NULL OR a.fechaHora <= :end)
             AND (:user IS NULL OR a.emailUsuario = :user)
             AND (:search IS NULL OR\s
                  LOWER(a.emailUsuario) LIKE LOWER(CONCAT('%', :search, '%')) OR
                  LOWER(a.accion) LIKE LOWER(CONCAT('%', :search, '%'))
             )
            \s""")
    Page<AuditLog> findByFilters(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("user") String user,
            @Param("search") String search,
            Pageable pageable
    );

    // Último log registrado (para encadenar el hashAnterior del siguiente evento)
    @Query("SELECT a FROM AuditLog a ORDER BY a.idAuditLog DESC")
    List<AuditLog> findTopByOrderByIdAuditLogDesc(Pageable pageable);

    // Todos los logs en orden ascendente (para verificar la cadena de hashes)
    @Query("SELECT a FROM AuditLog a ORDER BY a.idAuditLog ASC")
    List<AuditLog> findAllOrderByIdAsc();

    Optional<AuditLog> findFirstByOrderByIdAuditLogAsc();
}
