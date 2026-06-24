package com.sifa.core_sifa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestoreJob {
    private String jobId;
    private String backupId;
    private String status;
    private Integer progress;
    private String message;
    private String errorMessage;
    private String scope;
    private String tempSchemaAuth;
    private String tempSchemaCore;
    private String safetySchemaAuth;
    private String safetySchemaCore;
}
