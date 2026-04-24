package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record ExportRequestDTO(
        @JsonProperty("report_content") Map<String, Object> reportContent,
        @JsonProperty("export_format") String exportFormat,
        @JsonProperty("export_sections") List<String> exportSections,
        @JsonProperty("page_style") String pageStyle,
        @JsonProperty("header_text") String headerText,
        @JsonProperty("footer_text") String footerText,
        @JsonProperty("show_page_numbers") boolean showPageNumbers,
        Map<String, Object> watermark
) {
}
