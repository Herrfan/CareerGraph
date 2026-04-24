package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeneratedReportDTO(
        boolean success,
        @JsonProperty("markdown_content") String markdownContent
) {
}
