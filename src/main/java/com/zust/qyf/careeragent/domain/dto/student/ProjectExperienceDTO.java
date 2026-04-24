package com.zust.qyf.careeragent.domain.dto.student;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProjectExperienceDTO(
        String name,
        String role,
        String description,
        @JsonProperty("tech_stacks") List<String> techStacks,
        String highlight
) {
}
