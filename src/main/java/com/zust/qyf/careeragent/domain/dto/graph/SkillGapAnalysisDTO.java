package com.zust.qyf.careeragent.domain.dto.graph;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SkillGapAnalysisDTO(
        @JsonProperty("target_job") String targetJob,
        @JsonProperty("matched_skills") List<String> matchedSkills,
        @JsonProperty("missing_skills") List<String> missingSkills,
        @JsonProperty("skill_match_score") double skillMatchScore,
        String summary
) {
}
