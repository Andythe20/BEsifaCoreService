package com.example.core_sifa.repository;

import com.example.core_sifa.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IAuditLogRepository extends JpaRepository<AuditLog, Integer> {

    // traer todas las auditorias por el email del usuario
    List<AuditLog> findByEmailUsuario(String emailUsuario);


}
