package com.zust.qyf.careeragent.domain.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;

import java.util.List;

public record MatchResultDTO(
        @JsonProperty("match_score") double matchScore,
        @JsonProperty("dimension_scores") DimensionScoreDTO dimensionScores,
        @JsonProperty("matched_job") JobProfileDTO matchedJob,
        List<String> recommendations
) {
}
