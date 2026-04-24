package com.zust.qyf.careeragent.domain.dto.graph;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GraphRoleContextDTO(
        @JsonProperty("job_title") String jobTitle,
        @JsonProperty("city_tier") String cityTier,
        @JsonProperty("salary_band") String salaryBand,
        @JsonProperty("required_skills") List<String> requiredSkills,
        @JsonProperty("preferred_certificates") List<String> preferredCertificates,
        @JsonProperty("top_abilities") List<String> topAbilities,
        @JsonProperty("related_roles") List<String> relatedRoles,
        String summary
) {
}
