package com.zust.qyf.careeragent.infrastructure.mysql.entity;

import com.zust.qyf.careeragent.domain.dto.report.GrowthPlanResponseDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "report_snapshot")
public class ReportSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id", nullable = false, unique = true, length = 64)
    private String snapshotId;

    @Column(name = "student_id", nullable = false, length = 128)
    private String studentId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "target_job", nullable = false, length = 255)
    private String targetJob;

    @Column(name = "matched_job_id", length = 128)
    private String matchedJobId;

    @Column(name = "matched_job_title", length = 255)
    private String matchedJobTitle;

    @Column(name = "markdown_content", columnDefinition = "longtext")
    private String markdownContent;

    @Convert(converter = GrowthPlanResponseDTOConverter.class)
    @Column(name = "growth_plan", columnDefinition = "json")
    private GrowthPlanResponseDTO growthPlan;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
