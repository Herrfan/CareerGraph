package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.infrastructure.graph.EnterpriseGraphService;
import com.zust.qyf.careeragent.infrastructure.knowledge.JobCatalogService;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.NormalizedJobRecordEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.NormalizedJobRecordRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnterpriseGraphBootstrapApplicationService {
    private final NormalizedJobRecordRepository normalizedJobRecordRepository;
    private final EnterpriseGraphService enterpriseGraphService;
    private final JobCatalogService jobCatalogService;

    public EnterpriseGraphBootstrapApplicationService(NormalizedJobRecordRepository normalizedJobRecordRepository,
                                                      EnterpriseGraphService enterpriseGraphService,
                                                      JobCatalogService jobCatalogService) {
        this.normalizedJobRecordRepository = normalizedJobRecordRepository;
        this.enterpriseGraphService = enterpriseGraphService;
        this.jobCatalogService = jobCatalogService;
    }

    public Map<String, Object> bootstrapFromMysql(boolean forceSync) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<NormalizedJobRecordEntity> activeRecords = normalizedJobRecordRepository.findByStatusOrderByIdAsc("active");
        result.put("force_sync", forceSync);
        result.put("mysql_active_records", activeRecords.size());

        long graphExistingRecords = enterpriseGraphService.countJobPostings();
        if (graphExistingRecords < 0) {
            result.put("graph_available", false);
            result.put("graph_existing_records", 0);
            result.put("graph_upserted_records", 0);
            result.put("status", "neo4j_unavailable");
            return result;
        }

        result.put("graph_available", true);
        result.put("graph_existing_records", graphExistingRecords);
        if (activeRecords.isEmpty()) {
            result.put("graph_upserted_records", 0);
            result.put("status", "no_active_records");
            return result;
        }

        if (!forceSync && graphExistingRecords > 0) {
            result.put("graph_upserted_records", 0);
            result.put("status", "skipped_graph_already_initialized");
            return result;
        }

        try {
            List<NormalizedJobRecord> records = activeRecords.stream().map(this::toRecord).toList();
            int importedCount = enterpriseGraphService.importJobs(records);
            result.put("graph_upserted_records", importedCount);
            result.put("status", "imported");
        } catch (Exception e) {
            result.put("graph_upserted_records", 0);
            result.put("status", "neo4j_import_failed");
            result.put("graph_error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> bootstrapFromPortraits(boolean forceSync) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<JobProfileDTO> portraitJobs = jobCatalogService.getJobs();
        long graphExistingRoles = enterpriseGraphService.countGenericRoles();

        result.put("force_sync", forceSync);
        result.put("portrait_jobs", portraitJobs.size());

        if (graphExistingRoles < 0) {
            result.put("graph_available", false);
            result.put("graph_existing_roles", 0);
            result.put("graph_upserted_roles", 0);
            result.put("status", "neo4j_unavailable");
            return result;
        }

        result.put("graph_available", true);
        result.put("graph_existing_roles", graphExistingRoles);

        if (portraitJobs.isEmpty()) {
            result.put("graph_upserted_roles", 0);
            result.put("status", "no_portrait_jobs");
            return result;
        }

        if (!forceSync && graphExistingRoles > 0) {
            result.put("graph_upserted_roles", 0);
            result.put("status", "skipped_portrait_graph_already_initialized");
            return result;
        }

        try {
            int importedCount = enterpriseGraphService.importPortraits(portraitJobs);
            result.put("graph_upserted_roles", importedCount);
            result.put("status", "portrait_graph_imported");
        } catch (Exception e) {
            result.put("graph_upserted_roles", 0);
            result.put("status", "portrait_graph_import_failed");
            result.put("graph_error", e.getMessage());
        }
        return result;
    }

    private NormalizedJobRecord toRecord(NormalizedJobRecordEntity entity) {
        return new NormalizedJobRecord(
                entity.getSourceId(),
                entity.getSourcePlatform(),
                entity.getSourceUrl(),
                entity.getRowHash(),
                entity.getCompanyName(),
                entity.getCompanySize(),
                entity.getCompanyType(),
                entity.getCompanyIntro(),
                entity.getJobTitle(),
                entity.getJobCode(),
                entity.getJobCategory(),
                entity.getIndustry(),
                entity.getWorkAddress(),
                entity.getCity(),
                entity.getSalaryText(),
                entity.getSalaryMin(),
                entity.getSalaryMax(),
                entity.getEducationLevel(),
                entity.getExperienceText(),
                entity.getJobDescription(),
                entity.getSkills(),
                entity.getRequiredCertificates(),
                entity.getPublishedAt(),
                entity.getCrawledAt(),
                Boolean.TRUE,
                null,
                entity.getConfidenceScore(),
                entity.getFieldCompletenessScore(),
                entity.getCleanSource()
        );
    }
}
