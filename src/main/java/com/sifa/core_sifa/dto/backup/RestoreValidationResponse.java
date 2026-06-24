package com.sifa.core_sifa.dto.backup;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class RestoreValidationResponse {
    private boolean valid;
    private String backupId;
    private String mysqlVersion;
    private List<String> schemas;
    private List<String> errors;
    private List<String> warnings;

    public static RestoreValidationResponse valid(String backupId, String mysqlVersion,
                                                   List<String> schemas, List<String> warnings) {
        return RestoreValidationResponse.builder()
                .valid(true)
                .backupId(backupId)
                .mysqlVersion(mysqlVersion)
                .schemas(schemas)
                .errors(List.of())
                .warnings(warnings)
                .build();
    }

    public static RestoreValidationResponse invalid(List<String> errors) {
        return RestoreValidationResponse.builder()
                .valid(false)
                .errors(errors)
                .warnings(List.of())
                .build();
    }
}
