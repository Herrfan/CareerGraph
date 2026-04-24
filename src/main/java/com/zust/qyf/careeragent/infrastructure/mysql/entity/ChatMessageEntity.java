package com.zust.qyf.careeragent.infrastructure.mysql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_message")
public class ChatMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "conversation_key", nullable = false, length = 128)
    private String conversationKey;

    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false, columnDefinition = "longtext")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
