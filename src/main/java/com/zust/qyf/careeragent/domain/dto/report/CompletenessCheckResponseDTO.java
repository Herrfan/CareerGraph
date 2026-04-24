package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CompletenessCheckResponseDTO(
        boolean success,
        @JsonProperty("completeness_score") double completenessScore,
        @JsonProperty("completed_items") List<String> completedItems,
        @JsonProperty("missing_items") List<String> missingItems,
        @JsonProperty("suggested_items") List<String> suggestedItems
) {
}
