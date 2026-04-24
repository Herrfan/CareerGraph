package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GrowthPlanResponseDTO(
        boolean success,
        @JsonProperty("short_term_plan") GrowthPlanDTO shortTermPlan,
        @JsonProperty("mid_term_plan") GrowthPlanDTO midTermPlan
) {
}
