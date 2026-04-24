package com.zust.qyf.careeragent.infrastructure.mysql.repository;

import com.zust.qyf.careeragent.infrastructure.mysql.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {
    Optional<UserSessionEntity> findByToken(String token);
}
