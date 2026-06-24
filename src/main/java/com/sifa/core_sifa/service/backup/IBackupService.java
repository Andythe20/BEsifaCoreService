package com.sifa.core_sifa.service.backup;

import com.sifa.core_sifa.dto.backup.BackupJobResponse;
import com.sifa.core_sifa.dto.backup.BackupListResponse;
import com.sifa.core_sifa.dto.backup.DownloadResponse;

import java.nio.file.Path;
import java.util.List;

public interface IBackupService {
    BackupJobResponse createFullBackup(String createdBy);
    BackupJobResponse getJobStatus(String jobId);
    List<BackupListResponse> listBackups();
    DownloadResponse downloadBackup(String backupId);
    void deleteBackup(String backupId);
    BackupJobResponse uploadRestore(Path zipPath);
    BackupJobResponse uploadBackup(Path zipPath);
    BackupJobResponse uploadBackup(Path zipPath, String createdBy, String description);
}
