package com.zust.qyf.careeragent.domain.dto.importer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record NormalizedJobRecord(
        @JsonProperty("source_id") String sourceId,
        @JsonProperty("source_platform") String sourcePlatform,
        @JsonProperty("source_url") String sourceUrl,
        @JsonProperty("row_hash") String rowHash,
        @JsonProperty("company_name") String companyName,
        @JsonProperty("company_size") String companySize,
        @JsonProperty("company_type") String companyType,
        @JsonProperty("company_intro") String companyIntro,
        @JsonProperty("job_title") String jobTitle,
        @JsonProperty("job_code") String jobCode,
        @JsonProperty("job_category") String jobCategory,
        String industry,
        @JsonProperty("work_address") String workAddress,
        String city,
        @JsonProperty("salary_text") String salaryText,
        @JsonProperty("salary_min") Integer salaryMin,
        @JsonProperty("salary_max") Integer salaryMax,
        @JsonProperty("education_level") String educationLevel,
        @JsonProperty("experience_text") String experienceText,
        @JsonProperty("job_description") String jobDescription,
        List<String> skills,
        @JsonProperty("required_certificates") List<String> requiredCertificates,
        @JsonProperty("published_at") String publishedAt,
        @JsonProperty("crawled_at") String crawledAt,
        @JsonProperty("is_valid") Boolean isValid,
        @JsonProperty("invalid_reason") String invalidReason,
        @JsonProperty("confidence_score") Double confidenceScore,
        @JsonProperty("field_completeness_score") Double fieldCompletenessScore,
        @JsonProperty("clean_source") String cleanSource
) {
    public boolean isImportable() {
        return Boolean.TRUE.equals(isValid)
                && notBlank(sourceId)
                && notBlank(companyName)
                && notBlank(jobTitle);
    }

    public List<String> safeSkills() {
        return skills == null ? List.of() : skills.stream().filter(NormalizedJobRecord::notBlank).distinct().toList();
    }

    public List<String> safeRequiredCertificates() {
        if (requiredCertificates == null || requiredCertificates.isEmpty()) {
            return List.of("无");
        }
        List<String> values = requiredCertificates.stream().filter(NormalizedJobRecord::notBlank).distinct().toList();
        return values.isEmpty() ? List.of("无") : values;
    }

    public String companyKey() {
        return normalize(companyName);
    }

    public String categoryKey() {
        return normalize(jobCategory);
    }

    public String cityKey() {
        return normalize(city);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("\\s+", "_");
    }
}
