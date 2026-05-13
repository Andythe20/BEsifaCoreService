package com.sifa.core_sifa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sifa.core_sifa.model.AuditLog;

import java.util.List;

@Repository
public interface IAuditLogRepository extends JpaRepository<AuditLog, Integer> {

    // traer todas las auditorias por el email del usuario
    List<AuditLog> findByEmailUsuario(String emailUsuario);


}
