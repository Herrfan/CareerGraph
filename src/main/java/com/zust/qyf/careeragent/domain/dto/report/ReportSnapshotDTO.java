package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ReportSnapshotDTO(
        @JsonProperty("snapshot_id") String snapshotId,
        @JsonProperty("student_id") String studentId,
        @JsonProperty("target_job") String targetJob,
        @JsonProperty("matched_job_id") String matchedJobId,
        @JsonProperty("matched_job_title") String matchedJobTitle,
        @JsonProperty("markdown_content") String markdownContent,
        @JsonProperty("growth_plan") GrowthPlanResponseDTO growthPlan,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
