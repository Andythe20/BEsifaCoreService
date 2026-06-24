package com.sifa.core_sifa.dto.backup;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RestoreJobResponse {
    private String jobId;
    private String backupId;
    private String status;
    private Integer progress;
    private String message;
}
