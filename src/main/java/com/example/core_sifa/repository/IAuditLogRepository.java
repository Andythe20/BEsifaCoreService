package com.example.core_sifa.repository;

import com.example.core_sifa.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAuditLogRepository extends JpaRepository<AuditLog, Integer> {
}
