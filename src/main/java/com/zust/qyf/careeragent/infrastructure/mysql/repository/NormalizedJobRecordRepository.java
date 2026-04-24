package com.zust.qyf.careeragent.infrastructure.mysql.repository;

import com.zust.qyf.careeragent.infrastructure.mysql.entity.NormalizedJobRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NormalizedJobRecordRepository extends JpaRepository<NormalizedJobRecordEntity, Long> {
    Optional<NormalizedJobRecordEntity> findBySourceId(String sourceId);

    long countByStatus(String status);

    List<NormalizedJobRecordEntity> findByStatusOrderByIdAsc(String status);

    List<NormalizedJobRecordEntity> findAllByOrderByIdAsc();

    @Query("""
            select n
            from NormalizedJobRecordEntity n
            where n.status = :status
              and (n.cleanStatus is null or n.cleanStatus <> :expectedStatus)
            order by n.id asc
            """)
    List<NormalizedJobRecordEntity> findCleanupCandidates(@Param("status") String status,
                                                          @Param("expectedStatus") String expectedStatus,
                                                          Pageable pageable);
}
