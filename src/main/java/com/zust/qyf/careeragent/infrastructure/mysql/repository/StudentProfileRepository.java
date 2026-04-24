package com.zust.qyf.careeragent.infrastructure.mysql.repository;

import com.zust.qyf.careeragent.infrastructure.mysql.entity.StudentProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, Long> {
    Optional<StudentProfileEntity> findByStudentId(String studentId);

    Optional<StudentProfileEntity> findByUserId(Long userId);
}
