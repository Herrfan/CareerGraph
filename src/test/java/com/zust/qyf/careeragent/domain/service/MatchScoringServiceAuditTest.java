package com.zust.qyf.careeragent.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.application.CareerAiDecisionService;
import com.zust.qyf.careeragent.application.JobPortraitAssembler;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.match.JobMatchDTO;
import com.zust.qyf.careeragent.domain.dto.student.BasicInfoDTO;
import com.zust.qyf.careeragent.domain.dto.student.JobPreferenceDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import com.zust.qyf.careeragent.infrastructure.knowledge.AbilityWeightsService;
import com.zust.qyf.careeragent.infrastructure.knowledge.JobCatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchScoringServiceAuditTest {

    @Test
    void javaIntentionShouldBringJavaRoleIntoTopMatches() {
        ObjectMapper objectMapper = new ObjectMapper();
        AbilityWeightsService abilityWeightsService = new AbilityWeightsService(objectMapper);
        JobPortraitAssembler assembler = new JobPortraitAssembler();
        JobCatalogService jobCatalogService = new JobCatalogService(abilityWeightsService, objectMapper, assembler);
        CareerAiDecisionService careerAiDecisionService = Mockito.mock(CareerAiDecisionService.class);
        Mockito.when(careerAiDecisionService.resolveProfileIntent(Mockito.any(), Mockito.anyList())).thenReturn("Java开发");
        Mockito.when(careerAiDecisionService.refineConcreteSkills(Mockito.any())).thenReturn(List.of());
        MatchScoringService matchScoringService = new MatchScoringService(careerAiDecisionService);

        StudentProfileDTO profile = new StudentProfileDTO(
                "demo-java",
                new BasicInfoDTO("demo", "本科", "软件工程", "ZUST", "2026"),
                List.of("Java", "Spring Boot", "MySQL", "Redis"),
                List.of(),
                null,
                new JobPreferenceDTO("Java开发", "10K-15K", "杭州"),
                null,
                List.of(),
                List.of()
        );

        List<JobProfileDTO> jobs = jobCatalogService.getJobs();
        List<JobMatchDTO> topMatches = matchScoringService.calculateTopMatches(profile, jobs, 5);

        assertTrue(topMatches.stream().anyMatch(match ->
                        match.job().title() != null && match.job().title().toLowerCase().contains("java")),
                "top matches should contain Java role for a clear Java-intention profile");
    }
}
