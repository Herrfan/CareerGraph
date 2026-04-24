package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.report.GrowthPlanResponseDTO;
import com.zust.qyf.careeragent.domain.dto.report.ReportSnapshotDTO;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.ReportSnapshotEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.ReportSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReportSnapshotService {
    private final ReportSnapshotRepository reportSnapshotRepository;

    public ReportSnapshotService(ReportSnapshotRepository reportSnapshotRepository) {
        this.reportSnapshotRepository = reportSnapshotRepository;
    }

    public ReportSnapshotDTO saveOrUpdate(Long userId,
                                          String studentId,
                                          String targetJob,
                                          String matchedJobId,
                                          String matchedJobTitle,
                                          String markdownContent,
                                          GrowthPlanResponseDTO growthPlan) {
        ReportSnapshotEntity entity = reportSnapshotRepository
                .findFirstByUserIdAndTargetJobOrderByUpdatedAtDesc(userId, targetJob)
                .orElseGet(ReportSnapshotEntity::new);

        if (entity.getSnapshotId() == null || entity.getSnapshotId().isBlank()) {
            entity.setSnapshotId(UUID.randomUUID().toString());
            entity.setCreatedAt(LocalDateTime.now());
        }

        entity.setUserId(userId);
        entity.setStudentId(studentId);
        entity.setTargetJob(targetJob);
        entity.setMatchedJobId(matchedJobId);
        entity.setMatchedJobTitle(matchedJobTitle);
        if (markdownContent != null && !markdownContent.isBlank()) {
            entity.setMarkdownContent(markdownContent);
        }
        if (growthPlan != null) {
            entity.setGrowthPlan(growthPlan);
        }
        entity.setUpdatedAt(LocalDateTime.now());

        reportSnapshotRepository.save(entity);
        return toDto(entity);
    }

    public Optional<ReportSnapshotDTO> getLatest(Long userId, String targetJob) {
        return reportSnapshotRepository.findFirstByUserIdAndTargetJobOrderByUpdatedAtDesc(userId, targetJob)
                .map(this::toDto);
    }

    public List<ReportSnapshotDTO> listByUser(Long userId) {
        return reportSnapshotRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private ReportSnapshotDTO toDto(ReportSnapshotEntity entity) {
        return new ReportSnapshotDTO(
                entity.getSnapshotId(),
                entity.getStudentId(),
                entity.getTargetJob(),
                entity.getMatchedJobId(),
                entity.getMatchedJobTitle(),
                entity.getMarkdownContent(),
                entity.getGrowthPlan(),
                entity.getUpdatedAt()
        );
    }
}
