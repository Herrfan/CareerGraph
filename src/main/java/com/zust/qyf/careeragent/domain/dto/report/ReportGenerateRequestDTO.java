package com.zust.qyf.careeragent.domain.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;

public record ReportGenerateRequestDTO(
        @JsonProperty("student_profile") StudentProfileDTO studentProfile,
        @JsonProperty("job_id") String jobId,
        @JsonProperty("target_job") String targetJob
) {
}
