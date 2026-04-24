package com.zust.qyf.careeragent.web;

import com.zust.qyf.careeragent.application.EnterpriseKnowledgeImportApplicationService;
import com.zust.qyf.careeragent.application.KnowledgeBaseExportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class EnterpriseDataController {
    private final EnterpriseKnowledgeImportApplicationService enterpriseKnowledgeImportApplicationService;
    private final KnowledgeBaseExportService knowledgeBaseExportService;

    public EnterpriseDataController(EnterpriseKnowledgeImportApplicationService enterpriseKnowledgeImportApplicationService,
                                    KnowledgeBaseExportService knowledgeBaseExportService) {
        this.enterpriseKnowledgeImportApplicationService = enterpriseKnowledgeImportApplicationService;
        this.knowledgeBaseExportService = knowledgeBaseExportService;
    }

    @PostMapping("/import-normalized-jobs")
    public Map<String, Object> importNormalizedJobs(@RequestParam("path") String path) {
        return enterpriseKnowledgeImportApplicationService.importNormalizedJobs(path);
    }

    @GetMapping("/export-knowledge-base")
    public Map<String, Object> exportKnowledgeBase(@RequestParam(name = "computer_only", defaultValue = "true") boolean computerOnly) {
        return knowledgeBaseExportService.exportKnowledgeBase(computerOnly);
    }
}
