package com.zust.qyf.careeragent.domain.dto.student;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record StudentProfileEvaluationDTO(
        @JsonProperty("completeness_score") double completenessScore,
        @JsonProperty("competitiveness_score") double competitivenessScore,
        Map<String, Double> dimensions
) {
}

