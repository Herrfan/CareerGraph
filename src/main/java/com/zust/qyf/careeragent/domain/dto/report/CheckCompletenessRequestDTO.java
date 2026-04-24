package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record CheckCompletenessRequestDTO(
        @JsonProperty("report_content") Map<String, Object> reportContent
) {
}
