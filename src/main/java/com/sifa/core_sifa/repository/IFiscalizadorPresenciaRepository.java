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
     * Busca los inspectores que han enviado un latido dentro del rango de tiempo válido.
     */
    @Query("SELECT i FROM FiscalizadorPresencia i WHERE i.ultimaConexion >= :tiempoCorte")
    Page<FiscalizadorPresencia> findFiscalizadorActivos(@Param("tiempoCorte") LocalDateTime tiempoCorte, Pageable pageable);
}
