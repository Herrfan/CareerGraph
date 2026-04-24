package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record PolishReportRequestDTO(
        @JsonProperty("report_content") Map<String, Object> reportContent,
        @JsonProperty("polish_scope") String polishScope,
        @JsonProperty("polish_style") String polishStyle
) {
}
