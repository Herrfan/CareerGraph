package com.zust.qyf.careeragent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.ai.llm.AIService;
import com.zust.qyf.careeragent.domain.dto.student.BasicInfoDTO;
import com.zust.qyf.careeragent.domain.dto.student.JobPreferenceDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import com.zust.qyf.careeragent.infrastructure.knowledge.JobCatalogService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CareerAssistantWorkflowService {
    private final AIService aiService;
    private final EnterpriseGraphInsightsApplicationService enterpriseGraphInsightsApplicationService;
    private final LightRagKnowledgeService lightRagKnowledgeService;
    private final JobCatalogService jobCatalogService;
    private final ObjectMapper objectMapper;

    public CareerAssistantWorkflowService(AIService aiService,
                                          EnterpriseGraphInsightsApplicationService enterpriseGraphInsightsApplicationService,
                                          LightRagKnowledgeService lightRagKnowledgeService,
                                          JobCatalogService jobCatalogService,
                                          ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.enterpriseGraphInsightsApplicationService = enterpriseGraphInsightsApplicationService;
        this.lightRagKnowledgeService = lightRagKnowledgeService;
        this.jobCatalogService = jobCatalogService;
        this.objectMapper = objectMapper;
    }

    public String answerWithHistory(String history, String userMessage, String rawContext) {
        Grounding grounding = buildGrounding(userMessage, rawContext);
        return aiService.chatWithGroundedHistory(history, userMessage, grounding.render());
    }

    public String answerStateless(String userMessage, String rawContext) {
        Grounding grounding = buildGrounding(userMessage, rawContext);
        return aiService.chatWithoutMemoryWithGrounding(userMessage, grounding.render());
    }

    public List<com.zust.qyf.careeragent.domain.dto.knowledge.KnowledgeSnippetDTO> searchKnowledge(String query, String targetJob) {
        return lightRagKnowledgeService.search(query, targetJob, 6);
    }

    private Grounding buildGrounding(String userMessage, String rawContext) {
        StudentProfileDTO studentProfile = parseStudentProfile(rawContext);
        String targetJob = resolveTargetJob(studentProfile, userMessage);
        String graphContext = enterpriseGraphInsightsApplicationService.buildCareerChatContext(studentProfile, targetJob);
        String lightRagContext = lightRagKnowledgeService.buildPromptContext(
                safe(userMessage) + "\n" + safe(rawContext),
                targetJob,
                4
        );
        return new Grounding(rawContext, targetJob, graphContext, lightRagContext);
    }

    private StudentProfileDTO parseStudentProfile(String rawContext) {
        if (rawContext == null || rawContext.isBlank()) {
            return null;
        }

        try {
            JsonNode node = objectMapper.readTree(rawContext);
            BasicInfoDTO basicInfo = objectMapper.treeToValue(node.path("basicInfo"), BasicInfoDTO.class);
            JobPreferenceDTO jobPreference = objectMapper.treeToValue(node.path("jobPreference"), JobPreferenceDTO.class);
            List<String> skills = new java.util.ArrayList<>();
            if (node.path("skills").isArray()) {
                node.path("skills").forEach(item -> {
                    String skill = item.asText("");
                    if (!skill.isBlank()) {
                        skills.add(skill);
                    }
                });
            }
            return new StudentProfileDTO("", basicInfo, skills, List.of(), null, jobPreference, null, List.of(), List.of());
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveTargetJob(StudentProfileDTO studentProfile, String userMessage) {
        if (studentProfile != null
                && studentProfile.jobPreference() != null
                && notBlank(studentProfile.jobPreference().expectedPosition())) {
            return studentProfile.jobPreference().expectedPosition();
        }
        if (!notBlank(userMessage)) {
            return "";
        }

        return jobCatalogService.searchSimilarJobs(userMessage, 1).stream()
                .findFirst()
                .filter(job -> {
                    String normalizedMessage = normalize(userMessage);
                    String normalizedTitle = normalize(job.title());
                    return normalizedMessage.contains(normalizedTitle) || normalizedTitle.contains(normalizedMessage);
                })
                .map(job -> job.title())
                .orElse("");
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").replace("/", "").replace("+", "");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record Grounding(String rawContext, String targetJob, String graphContext, String lightRagContext) {
        private String render() {
            StringBuilder builder = new StringBuilder();
            if (rawContext != null && !rawContext.isBlank()) {
                builder.append("[User Context]\n").append(rawContext.trim()).append("\n\n");
            }
            if (targetJob != null && !targetJob.isBlank()) {
                builder.append("[Resolved Target Job]\n").append(targetJob.trim()).append("\n\n");
            }
            if (graphContext != null && !graphContext.isBlank()) {
                builder.append(graphContext.trim()).append("\n\n");
            }
            if (lightRagContext != null && !lightRagContext.isBlank()) {
                builder.append(lightRagContext.trim()).append('\n');
            }
            return builder.toString().trim();
        }
    }
}
