package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.NormalizedJobRecordEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.NormalizedJobRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnterpriseJobRecordPersistenceService {
    private final NormalizedJobRecordRepository normalizedJobRecordRepository;
    private final OllamaJobCleanupService ollamaJobCleanupService;

    public EnterpriseJobRecordPersistenceService(NormalizedJobRecordRepository normalizedJobRecordRepository,
                                                 OllamaJobCleanupService ollamaJobCleanupService) {
        this.normalizedJobRecordRepository = normalizedJobRecordRepository;
        this.ollamaJobCleanupService = ollamaJobCleanupService;
    }

    public int upsertRecords(List<NormalizedJobRecord> records) {
        int persisted = 0;
        for (NormalizedJobRecord record : records) {
            NormalizedJobRecordEntity entity = normalizedJobRecordRepository.findBySourceId(record.sourceId())
                    .orElseGet(NormalizedJobRecordEntity::new);

            entity.setSourceId(record.sourceId());
            entity.setSourcePlatform(record.sourcePlatform());
            entity.setSourceUrl(record.sourceUrl());
            entity.setRowHash(record.rowHash());
            entity.setCompanyName(record.companyName());
            entity.setCompanySize(record.companySize());
            entity.setCompanyType(record.companyType());
            entity.setCompanyIntro(record.companyIntro());
            entity.setJobTitle(record.jobTitle());
            entity.setJobCode(record.jobCode());
            entity.setJobCategory(record.jobCategory());
            entity.setIndustry(record.industry());
            entity.setWorkAddress(record.workAddress());
            entity.setCity(record.city());
            entity.setSalaryText(record.salaryText());
            entity.setSalaryMin(record.salaryMin());
            entity.setSalaryMax(record.salaryMax());
            entity.setEducationLevel(record.educationLevel());
            entity.setExperienceText(record.experienceText());
            entity.setJobDescription(record.jobDescription());
            entity.setSkills(record.safeSkills());
            entity.setRequiredCertificates(record.safeRequiredCertificates());
            entity.setPublishedAt(record.publishedAt());
            entity.setCrawledAt(record.crawledAt());
            entity.setIsValid(record.isValid());
            entity.setInvalidReason(record.invalidReason());
            entity.setConfidenceScore(record.confidenceScore());
            entity.setFieldCompletenessScore(record.fieldCompletenessScore());
            entity.setStatus(record.isImportable() ? "active" : "rejected");
            entity.setCleanSource(record.cleanSource());

            entity = ollamaJobCleanupService.cleanupIfNeeded(entity);

            normalizedJobRecordRepository.save(entity);
            persisted++;
        }
        return persisted;
    }
}
