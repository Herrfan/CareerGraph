package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record ReportExportResponseDTO(
        boolean success,
        @JsonProperty("export_id") String exportId,
        String filename,
        String format,
        String content,
        @JsonProperty("content_type") String contentType,
        @JsonProperty("available_sections") List<String> availableSections,
        Map<String, Object> metadata
) {
}
