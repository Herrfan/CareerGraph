package com.zust.qyf.careeragent.domain.dto.chat;

import java.time.LocalDateTime;

public record ChatMessageDTO(
        String conversationKey,
        String role,
        String content,
        LocalDateTime createdAt
) {
}
