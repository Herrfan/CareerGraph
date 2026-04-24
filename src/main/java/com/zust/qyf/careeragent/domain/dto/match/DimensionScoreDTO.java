package com.zust.qyf.careeragent.domain.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DimensionScoreDTO(
        @JsonProperty("basic_requirements") double basicRequirements,
        @JsonProperty("professional_skills") double professionalSkills,
        @JsonProperty("professional_quality") double professionalQuality,
        @JsonProperty("growth_potential") double growthPotential
) {
}
