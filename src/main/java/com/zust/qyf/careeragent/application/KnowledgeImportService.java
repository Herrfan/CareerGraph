package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import com.zust.qyf.careeragent.domain.dto.job.JobOptionDTO;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.infrastructure.knowledge.JobCatalogService;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class KnowledgeImportService {
    private final EnterpriseJobCatalogQueryService enterpriseJobCatalogQueryService;
    private final JobCatalogService jobCatalogService;
    private final RawExcelJobPreprocessService rawExcelJobPreprocessService;
    private final EnterpriseKnowledgeImportApplicationService enterpriseKnowledgeImportApplicationService;

    public KnowledgeImportService(EnterpriseJobCatalogQueryService enterpriseJobCatalogQueryService,
                                  JobCatalogService jobCatalogService,
                                  RawExcelJobPreprocessService rawExcelJobPreprocessService,
                                  EnterpriseKnowledgeImportApplicationService enterpriseKnowledgeImportApplicationService) {
        this.enterpriseJobCatalogQueryService = enterpriseJobCatalogQueryService;
        this.jobCatalogService = jobCatalogService;
        this.rawExcelJobPreprocessService = rawExcelJobPreprocessService;
        this.enterpriseKnowledgeImportApplicationService = enterpriseKnowledgeImportApplicationService;
    }

    public List<JobProfileDTO> getJobs(int skip, int limit) {
        int safeSkip = Math.max(skip, 0);
        int safeLimit = Math.max(limit, 1);
        List<JobProfileDTO> jobs = getAllJobs();
        if (safeSkip >= jobs.size()) {
            return List.of();
        }
        int endIndex = Math.min(jobs.size(), safeSkip + safeLimit);
        return jobs.subList(safeSkip, endIndex);
    }

    public Optional<JobProfileDTO> getJob(String jobId) {
        return jobCatalogService.getJobById(jobId);
    }

    public long importJobs() {
        return jobCatalogService.importJobs();
    }

    public List<JobProfileDTO> searchSimilarJobs(String query, int limit) {
        return jobCatalogService.searchSimilarJobs(query, Math.max(limit, 1));
    }

    public List<JobProfileDTO> getAllJobs() {
        return jobCatalogService.getJobs();
    }

    public List<JobOptionDTO> getJobOptions(boolean computerOnly) {
        Map<String, JobOptionDTO> optionsByTitle = new LinkedHashMap<>();
        List<JobProfileDTO> sourceJobs = computerOnly
                ? ComputerCategoryPolicy.filterComputerJobs(getAllJobs())
                : getAllJobs();
        for (JobProfileDTO job : sourceJobs) {
            String title = trim(job.title());
            if (title.isBlank()) {
                continue;
            }
            String category = trim(job.category());
            String normalizedTitle = normalize(title);
            JobOptionDTO existing = optionsByTitle.get(normalizedTitle);
            if (existing == null || ("未分类".equals(existing.category()) && !category.isBlank())) {
                optionsByTitle.put(
                        normalizedTitle,
                        new JobOptionDTO(title, category.isBlank() ? "未分类" : category)
                );
            }
        }
        return optionsByTitle.values().stream()
                .sorted(Comparator.comparing((JobOptionDTO option) -> normalize(option.category()))
                        .thenComparing(option -> normalize(option.title())))
                .toList();
    }

    public List<JobOptionDTO> getJobOptions() {
        return getJobOptions(false);
    }

    public List<String> getDistinctJobTitles() {
        return getJobOptions(false).stream()
                .map(JobOptionDTO::title)
                .toList();
    }

    public Map<String, Object> getJobSourceStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_records", jobCatalogService.getJobs().size());
        result.put("active_records", jobCatalogService.getJobs().size());
        result.put("using_enterprise_source", false);
        result.put("prefer_active_records", false);
        result.put("fallback_catalog_records", jobCatalogService.getJobs().size());
        return result;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private Path findRawExcel() {
        try (var stream = Files.list(Path.of("."))) {
            return stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".xls") || name.endsWith(".xlsx");
                    })
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
