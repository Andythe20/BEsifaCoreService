package com.sifa.core_sifa.service.audits;

import com.sifa.core_sifa.dto.audit.AuditLogRequestDTO;
import com.sifa.core_sifa.dto.audit.AuditLogResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface IAuditLogService {

    AuditLogResponseDTO findById(Long id);

    Page<AuditLogResponseDTO> findAll(
            LocalDate startDate,
            LocalDate endDate,
            String user,
            String search,
            Pageable pageable
    );

    void registrarLog(AuditLogRequestDTO request);

}
