package com.zust.qyf.careeragent.infrastructure.mysql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "normalized_job_record")
public class NormalizedJobRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false, unique = true, length = 128)
    private String sourceId;

    @Column(name = "source_platform", length = 64)
    private String sourcePlatform;

    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "row_hash", length = 64)
    private String rowHash;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "company_size", length = 128)
    private String companySize;

    @Column(name = "company_type", length = 128)
    private String companyType;

    @Column(name = "company_intro", columnDefinition = "longtext")
    private String companyIntro;

    @Column(name = "job_title", nullable = false, length = 255)
    private String jobTitle;

    @Column(name = "job_code", length = 128)
    private String jobCode;

    @Column(name = "job_category", length = 128)
    private String jobCategory;

    @Column(length = 255)
    private String industry;

    @Column(name = "work_address", length = 512)
    private String workAddress;

    @Column(length = 128)
    private String city;

    @Column(name = "salary_text", length = 128)
    private String salaryText;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "education_level", length = 64)
    private String educationLevel;

    @Column(name = "experience_text", length = 128)
    private String experienceText;

    @Column(name = "job_description", columnDefinition = "longtext")
    private String jobDescription;

    @Column(name = "display_markdown", columnDefinition = "longtext")
    private String displayMarkdown;

    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "json")
    private List<String> skills;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "required_certificates", columnDefinition = "json")
    private List<String> requiredCertificates;

    @Column(name = "published_at", length = 64)
    private String publishedAt;

    @Column(name = "crawled_at", length = 64)
    private String crawledAt;

    @Column(name = "is_valid", nullable = false)
    private Boolean isValid;

    @Column(name = "invalid_reason", length = 255)
    private String invalidReason;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "field_completeness_score")
    private Double fieldCompletenessScore;

    @Column(length = 32)
    private String status;

    @Column(name = "clean_status", length = 32)
    private String cleanStatus;

    @Column(name = "clean_source", length = 32)
    private String cleanSource;
}
