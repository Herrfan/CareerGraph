package com.zust.qyf.careeragent.infrastructure.mysql.repository;

import com.zust.qyf.careeragent.infrastructure.mysql.entity.StudentProfileHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentProfileHistoryRepository extends JpaRepository<StudentProfileHistoryEntity, Long> {
    List<StudentProfileHistoryEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
