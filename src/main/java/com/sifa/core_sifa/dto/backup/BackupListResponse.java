package com.sifa.core_sifa.dto.backup;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Builder
public class BackupListResponse {
    private String id;
    private String createdAt;
    private List<String> databases;
    private Long totalSizeBytes;
    private String description;
    private String source;
    private String createdBy;
}
