package com.zust.qyf.careeragent.web;

import com.zust.qyf.careeragent.application.KnowledgeImportService;
import com.zust.qyf.careeragent.domain.dto.job.JobOptionDTO;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final KnowledgeImportService knowledgeImportService;

    public JobController(KnowledgeImportService knowledgeImportService) {
        this.knowledgeImportService = knowledgeImportService;
    }

    @GetMapping
    public List<JobProfileDTO> getJobs(@RequestParam(defaultValue = "0") int skip,
                                       @RequestParam(defaultValue = "50") int limit) {
        return knowledgeImportService.getJobs(skip, limit);
    }

    @GetMapping("/titles")
    public List<String> getJobTitles() {
        return knowledgeImportService.getDistinctJobTitles();
    }

    @GetMapping("/options")
    public List<JobOptionDTO> getJobOptions(@RequestParam(name = "computer_only", defaultValue = "false") boolean computerOnly) {
        return knowledgeImportService.getJobOptions(computerOnly);
    }

    @GetMapping("/source-stats")
    public Map<String, Object> getJobSourceStats() {
        return knowledgeImportService.getJobSourceStats();
    }

    @GetMapping("/{jobId}")
    public JobProfileDTO getJob(@PathVariable String jobId) {
        return knowledgeImportService.getJob(jobId)
                .orElseThrow(() -> new IllegalArgumentException("未找到对应岗位: " + jobId));
    }

    @PostMapping("/import")
    public Map<String, Object> importJobs() {
        long count = knowledgeImportService.importJobs();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "岗位导入成功");
        response.put("count", count);
        return response;
    }

    @GetMapping("/search/similar")
    public List<JobProfileDTO> searchSimilarJobs(@RequestParam String query,
                                                 @RequestParam(name = "n_results", defaultValue = "5") int nResults) {
        return knowledgeImportService.searchSimilarJobs(query, nResults);
    }
}
