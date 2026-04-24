package com.zust.qyf.careeragent.domain.dto.student;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record StudentProfileSnapshotDTO(
        @JsonProperty("snapshot_id") String snapshotId,
        @JsonProperty("student_id") String studentId,
        @JsonProperty("profile_data") StudentProfileDTO profileData,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
}
