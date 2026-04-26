package com.example.core_sifa.repository;

import com.example.core_sifa.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAuditLogRepository extends JpaRepository<AuditLog, Integer> {
}
