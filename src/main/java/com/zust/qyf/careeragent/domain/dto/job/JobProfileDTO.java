package com.zust.qyf.careeragent.domain.dto.job;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record JobProfileDTO(
        @JsonProperty("job_id") String jobId,
        String title,
        String department,
        String description,
        @JsonProperty("description_markdown") String descriptionMarkdown,
        String category,
        @JsonProperty("required_skills") List<String> requiredSkills,
        @JsonProperty("required_certificates") List<String> requiredCertificates,
        @JsonProperty("ability_weights") Map<String, Double> abilityWeights,
        @JsonProperty("ability_portrait") Map<String, String> abilityPortrait,
        @JsonProperty("ability_priority") Map<String, Integer> abilityPriority,
        @JsonProperty("salary_range") String salaryRange,
        String city,
        @JsonProperty("experience_required") String experienceRequired,
        @JsonProperty("company_name") String companyName,
        @JsonProperty("work_address") String workAddress,
        String industry,
        @JsonProperty("company_size") String companySize,
        @JsonProperty("company_type") String companyType,
        @JsonProperty("job_code") String jobCode,
        @JsonProperty("company_intro") String companyIntro,
        @JsonProperty("portrait_scope") String portraitScope,
        @JsonProperty("city_tier") String cityTier,
        @JsonProperty("salary_band") String salaryBand,
        @JsonProperty("portrait_source_count") Integer portraitSourceCount,
        @JsonProperty("portrait_confidence") Double portraitConfidence,
        @JsonProperty("source_record_ids") List<String> sourceRecordIds,
        @JsonProperty("related_job_ids") List<String> relatedJobIds
) {
}
