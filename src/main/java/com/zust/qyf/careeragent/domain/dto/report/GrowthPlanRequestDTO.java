package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;

public record GrowthPlanRequestDTO(
        @JsonProperty("student_profile") StudentProfileDTO studentProfile,
        @JsonProperty("target_job") String targetJob
) {
}
