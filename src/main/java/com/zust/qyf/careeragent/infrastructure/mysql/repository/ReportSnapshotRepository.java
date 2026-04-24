package com.zust.qyf.careeragent.infrastructure.mysql.repository;

import com.zust.qyf.careeragent.infrastructure.mysql.entity.ReportSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportSnapshotRepository extends JpaRepository<ReportSnapshotEntity, Long> {
    Optional<ReportSnapshotEntity> findBySnapshotId(String snapshotId);

    Optional<ReportSnapshotEntity> findFirstByStudentIdAndTargetJobOrderByUpdatedAtDesc(String studentId, String targetJob);

    List<ReportSnapshotEntity> findByStudentIdOrderByUpdatedAtDesc(String studentId);

    Optional<ReportSnapshotEntity> findFirstByUserIdAndTargetJobOrderByUpdatedAtDesc(Long userId, String targetJob);

    List<ReportSnapshotEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
