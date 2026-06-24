package com.sifa.core_sifa.repository;

import com.sifa.core_sifa.model.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IBackupJobRepository extends JpaRepository<BackupJob, Long> {
    Optional<BackupJob> findByJobId(String jobId);
}
