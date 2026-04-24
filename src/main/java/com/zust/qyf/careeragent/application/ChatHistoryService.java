package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.chat.ChatMessageDTO;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.ChatMessageEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ChatHistoryService {
    private final ChatMessageRepository chatMessageRepository;

    public ChatHistoryService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public void saveUserMessage(Long userId, String conversationKey, String content) {
        save(userId, conversationKey, "user", content);
    }

    public void saveAssistantMessage(Long userId, String conversationKey, String content) {
        save(userId, conversationKey, "assistant", content);
    }

    public String buildRecentHistory(Long userId, String conversationKey) {
        List<ChatMessageEntity> messages = chatMessageRepository.findTop20ByUserIdAndConversationKeyOrderByCreatedAtDesc(userId, conversationKey);
        Collections.reverse(messages);
        StringBuilder builder = new StringBuilder();
        for (ChatMessageEntity message : messages) {
            builder.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }
        return builder.toString();
    }

    private void save(Long userId, String conversationKey, String role, String content) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setUserId(userId);
        entity.setConversationKey(conversationKey);
        entity.setRole(role);
        entity.setContent(content);
        entity.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(entity);
    }

    public List<ChatMessageDTO> listRecentMessages(Long userId) {
        return chatMessageRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(entity -> new ChatMessageDTO(
                        entity.getConversationKey(),
                        entity.getRole(),
                        entity.getContent(),
                        entity.getCreatedAt()
                ))
                .toList();
    }
}
