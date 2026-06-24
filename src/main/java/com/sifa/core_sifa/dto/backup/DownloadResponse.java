package com.sifa.core_sifa.dto.backup;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DownloadResponse {
    private String url;
}
