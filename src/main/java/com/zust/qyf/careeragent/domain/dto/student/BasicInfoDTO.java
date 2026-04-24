package com.zust.qyf.careeragent.domain.dto.student;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BasicInfoDTO(
        String name,
        String education,
        String major,
        String school,
        @JsonProperty("graduation_year") String graduationYear
) {
}
