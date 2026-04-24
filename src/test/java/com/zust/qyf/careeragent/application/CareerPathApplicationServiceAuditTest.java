package com.zust.qyf.careeragent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.path.CareerPathResponseDTO;
import com.zust.qyf.careeragent.domain.service.MatchScoringService;
import com.zust.qyf.careeragent.infrastructure.knowledge.AbilityWeightsService;
import com.zust.qyf.careeragent.infrastructure.knowledge.JobCatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CareerPathApplicationServiceAuditTest {

    @Test
    void allProcessedPortraitRolesProduceStructuredFamilyPaths() {
        ObjectMapper objectMapper = new ObjectMapper();
        AbilityWeightsService abilityWeightsService = new AbilityWeightsService(objectMapper);
        JobPortraitAssembler assembler = new JobPortraitAssembler();
        JobCatalogService jobCatalogService = new JobCatalogService(abilityWeightsService, objectMapper, assembler);
        List<JobProfileDTO> jobs = jobCatalogService.getJobs();

        KnowledgeImportService knowledgeImportService = Mockito.mock(KnowledgeImportService.class);
        CareerAiDecisionService careerAiDecisionService = Mockito.mock(CareerAiDecisionService.class);
        when(knowledgeImportService.getAllJobs()).thenReturn(jobs);
        when(knowledgeImportService.getJob(Mockito.anyString())).thenAnswer(invocation ->
                jobs.stream().filter(job -> job.jobId().equals(invocation.getArgument(0))).findFirst());
        MatchScoringService matchScoringService = new MatchScoringService(careerAiDecisionService);
        CareerFamilyPlannerService plannerService = new CareerFamilyPlannerService(knowledgeImportService, matchScoringService);
        CareerPathApplicationService service = new CareerPathApplicationService(plannerService);

        for (JobProfileDTO job : jobs) {
            CareerPathResponseDTO response = service.generateCareerPath(job.title(), null);

            List<String> verticalTitles = response.verticalPath().stream()
                    .map(item -> String.valueOf(item.getOrDefault("jobTitle", "")))
                    .filter(title -> !title.isBlank())
                    .toList();
            assertTrue(verticalTitles.size() >= 4, "vertical path too short for " + job.title());
            assertTrue(verticalTitles.stream().map(this::normalize).distinct().count() >= 4,
                    "vertical path lacks progression for " + job.title() + ": " + verticalTitles);

            List<Map<String, Object>> horizontal = response.horizontalPath();
            assertFalse(horizontal.isEmpty(), "horizontal family switches missing for " + job.title());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> paths = (List<Map<String, Object>>) horizontal.get(0).getOrDefault("paths", List.of());
            assertTrue(paths.size() >= 2, "switch paths too few for " + job.title());

            assertTrue(paths.stream().allMatch(path -> path.containsKey("switchReason")),
                    "switch paths should provide specific reasons for " + job.title());
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").replace("/", "").replace("+", "");
    }
}
