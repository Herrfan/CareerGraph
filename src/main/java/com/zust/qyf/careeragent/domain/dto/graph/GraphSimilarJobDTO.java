package com.zust.qyf.careeragent.domain.dto.graph;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GraphSimilarJobDTO(
        @JsonProperty("source_id") String sourceId,
        @JsonProperty("job_title") String jobTitle,
        @JsonProperty("company_name") String companyName,
        String city,
        String category,
        @JsonProperty("shared_skills") List<String> sharedSkills,
        @JsonProperty("similarity_score") double similarityScore
) {
}
