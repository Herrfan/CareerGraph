package com.zust.qyf.careeragent.infrastructure.mysql.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.ResumeDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ResumeDTOConverter implements AttributeConverter<ResumeDTO, String> {

    // Spring Boot 内置的 JSON 处理工具
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 动作 1：把 Java 对象转成 JSON 字符串（存入数据库时调用）
     */
    @Override
    public String convertToDatabaseColumn(ResumeDTO attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("简历DTO序列化为JSON失败", e);
        }
    }

    /**
     * 动作 2：把 JSON 字符串反转成 Java 对象（从数据库读取时调用）
     */
    @Override
    public ResumeDTO convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, ResumeDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("数据库JSON反序列化为简历DTO失败", e);
        }
    }
}