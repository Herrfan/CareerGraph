package com.zust.qyf.careeragent.domain.dto.student;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AbilityDescriptionsDTO(
        @JsonProperty("professional_skill") String professionalSkill,
        @JsonProperty("soft_skill") String softSkill,
        String certificate,
        String innovation,
        String learning,
        @JsonProperty("stress_tolerance") String stressTolerance,
        String communication,
        String internship
) {
}
