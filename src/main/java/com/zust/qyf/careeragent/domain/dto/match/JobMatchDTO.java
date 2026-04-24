package com.zust.qyf.careeragent.domain.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;

public record JobMatchDTO(
        JobProfileDTO job,
        @JsonProperty("match_score") double matchScore,
        @JsonProperty("dimension_scores") DimensionScoreDTO dimensionScores
) {
}
