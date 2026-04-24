package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeBaseExportService {
    private final KnowledgeImportService knowledgeImportService;
    private final JobPortraitAssembler jobPortraitAssembler;

    public KnowledgeBaseExportService(KnowledgeImportService knowledgeImportService,
                                      JobPortraitAssembler jobPortraitAssembler) {
        this.knowledgeImportService = knowledgeImportService;
        this.jobPortraitAssembler = jobPortraitAssembler;
    }

    public Map<String, Object> exportKnowledgeBase(boolean computerOnly) {
        List<JobProfileDTO> jobs = computerOnly
                ? ComputerCategoryPolicy.filterComputerJobs(knowledgeImportService.getAllJobs())
                : knowledgeImportService.getAllJobs();

        List<Map<String, Object>> items = jobs.stream()
                .map(this::toKnowledgeItem)
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("exported_at", OffsetDateTime.now().toString());
        payload.put("vector_model", "rule_vector_v1");
        payload.put("job_count", items.size());
        payload.put("jobs", items);
        return payload;
    }

    private Map<String, Object> toKnowledgeItem(JobProfileDTO job) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("job_id", job.jobId());
        item.put("job_name", job.title());
        item.put("category", job.category());
        item.put("portrait_scope", job.portraitScope());
        item.put("city_tier", job.cityTier());
        item.put("salary_band", job.salaryBand());
        item.put("portrait_source_count", job.portraitSourceCount());
        item.put("portrait_confidence", job.portraitConfidence());
        item.put("required_skills", job.requiredSkills());
        item.put("required_certificates", job.requiredCertificates());
        item.put("skill_vector", jobPortraitAssembler.buildSkillVector(job.requiredSkills(), job.requiredCertificates()));
        item.put("relation_ids", job.relatedJobIds());
        item.put("source_record_ids", job.sourceRecordIds());
        item.put("ability_weights", job.abilityWeights());
        return item;
    }
}
