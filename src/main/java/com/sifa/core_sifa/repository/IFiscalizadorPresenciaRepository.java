package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.model.FiscalizadorPresencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IFiscalizadorPresenciaRepository extends JpaRepository<FiscalizadorPresencia, String> {

    /**
     * Busca los inspectores que han enviado un latido dentro del rango de tiempo válido.
     */
    @Query("SELECT i FROM InspectorPresencia i WHERE i.ultimaConexion >= :tiempoCorte")
    List<FiscalizadorPresencia> findFiscalizadorActivos(@Param("tiempoCorte") LocalDateTime tiempoCorte);
}
