package com.zust.qyf.careeragent.domain.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AdminJobUpdateRequestDTO(
        @JsonProperty("company_name") String companyName,
        @JsonProperty("job_title") String jobTitle,
        @JsonProperty("job_category") String jobCategory,
        String city,
        String industry,
        @JsonProperty("salary_text") String salaryText,
        @JsonProperty("experience_text") String experienceText,
        @JsonProperty("education_level") String educationLevel,
        @JsonProperty("job_description") String jobDescription,
        List<String> skills,
        String status
) {
}
