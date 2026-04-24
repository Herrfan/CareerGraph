package com.zust.qyf.careeragent.infrastructure.mysql.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StudentProfileDTOConverter implements AttributeConverter<StudentProfileDTO, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(StudentProfileDTO attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize student profile", e);
        }
    }

    @Override
    public StudentProfileDTO convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, StudentProfileDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize student profile", e);
        }
    }
}
