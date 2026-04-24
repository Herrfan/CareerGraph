package com.zust.qyf.careeragent.infrastructure.mysql.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.report.GrowthPlanResponseDTO;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GrowthPlanResponseDTOConverter implements AttributeConverter<GrowthPlanResponseDTO, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(GrowthPlanResponseDTO attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize growth plan response", e);
        }
    }

    @Override
    public GrowthPlanResponseDTO convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, GrowthPlanResponseDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize growth plan response", e);
        }
    }
}
