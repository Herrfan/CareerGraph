package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;

public record SaveReportSnapshotRequestDTO(
        @JsonProperty("student_profile") StudentProfileDTO studentProfile,
        @JsonProperty("target_job") String targetJob,
        @JsonProperty("matched_job_id") String matchedJobId,
        @JsonProperty("matched_job_title") String matchedJobTitle,
        @JsonProperty("markdown_content") String markdownContent,
        @JsonProperty("growth_plan") GrowthPlanResponseDTO growthPlan
) {
}
