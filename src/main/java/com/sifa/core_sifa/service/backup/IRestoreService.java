package com.sifa.core_sifa.service.backup;

import com.sifa.core_sifa.dto.backup.RestoreJobResponse;
import com.sifa.core_sifa.dto.backup.RestoreValidationResponse;

public interface IRestoreService {
    RestoreJobResponse startRestore(String backupId, String scope);
    RestoreJobResponse getRestoreStatus(String jobId);
    RestoreValidationResponse validateBackup(String backupId);
    void cancelRestore(String jobId);
}
