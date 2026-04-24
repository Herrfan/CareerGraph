package com.zust.qyf.careeragent.domain.dto.match;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;

public record MatchRequestDTO(
        @JsonProperty("student_profile") StudentProfileDTO studentProfile,
        @JsonProperty("job_id") String jobId,
        String category,
        @JsonProperty("top_n") Integer topN
) {
}
