package com.zust.qyf.careeragent.domain.dto.student;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SoftAbilitiesDTO(
        int innovation,
        int learning,
        @JsonProperty("stress_tolerance") int stressTolerance,
        int communication,
        @JsonProperty("professional_skills") int professionalSkills,
        int certificates,
        int internship
) {
}
