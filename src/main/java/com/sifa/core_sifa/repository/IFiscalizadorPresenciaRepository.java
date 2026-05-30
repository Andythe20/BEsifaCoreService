package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.model.FiscalizadorPresencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IFiscalizadorPresenciaRepository extends JpaRepository<FiscalizadorPresencia, String> {

    /**
     * Busca los inspectores activos en los últimos 10 minutos,
     * devolviendo estrictamente su última ubicación conocida (un solo registro por inspector).
     */
    @Query("SELECT f FROM FiscalizadorPresencia f " +
            "WHERE f.ultimaConexion >= :tiempoCorte " +
            "AND f.ultimaConexion = (" +
            "    SELECT MAX(f2.ultimaConexion) " +
            "    FROM FiscalizadorPresencia f2 " +
            "    WHERE f2.emailUsuario = f.emailUsuario" +
            ")")
    Page<FiscalizadorPresencia> findFiscalizadorActivos(
            @Param("tiempoCorte") LocalDateTime tiempoCorte,
            Pageable pageable
    );
}
