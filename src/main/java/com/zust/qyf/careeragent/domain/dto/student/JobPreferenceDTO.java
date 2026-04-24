package com.zust.qyf.careeragent.domain.dto.student;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobPreferenceDTO(
        @JsonProperty("expected_position") String expectedPosition,
        @JsonProperty("expected_salary") String expectedSalary,
        @JsonProperty("expected_city") String expectedCity
) {
}
