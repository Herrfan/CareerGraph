package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record GrowthPlanDTO(
        @JsonProperty("learning_path") List<Map<String, Object>> learningPath,
        @JsonProperty("practice_arrangements") List<Map<String, Object>> practiceArrangements,
        @JsonProperty("evaluation_metrics") Map<String, Object> evaluationMetrics
) {
}
