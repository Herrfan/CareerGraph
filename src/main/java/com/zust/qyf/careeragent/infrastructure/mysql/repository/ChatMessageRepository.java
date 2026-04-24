package com.zust.qyf.careeragent.infrastructure.mysql.repository;

import com.zust.qyf.careeragent.infrastructure.mysql.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findTop20ByUserIdAndConversationKeyOrderByCreatedAtDesc(Long userId, String conversationKey);

    List<ChatMessageEntity> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
}
