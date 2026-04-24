package com.zust.qyf.careeragent.domain.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryMatchDTO(
        String category,
        @JsonProperty("match_score") double matchScore,
        @JsonProperty("job_count") int jobCount
) {
}
