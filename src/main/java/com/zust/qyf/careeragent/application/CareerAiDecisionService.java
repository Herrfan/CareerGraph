package com.zust.qyf.careeragent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CareerAiDecisionService {
    private final ChatClient pureChatClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> familyCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> skillCache = new ConcurrentHashMap<>();

    public CareerAiDecisionService(@Qualifier("pureChatClient") ChatClient pureChatClient,
                                   ObjectMapper objectMapper) {
        this.pureChatClient = pureChatClient;
        this.objectMapper = objectMapper;
    }

    public String resolveRoleFamily(JobProfileDTO job, List<String> candidates) {
        if (job == null || candidates == null || candidates.isEmpty()) {
            return "";
        }
        String cacheKey = "job|" + safe(job.title()) + "|" + safe(job.category());
        return familyCache.computeIfAbsent(cacheKey, key -> {
            String prompt = """
                    你是职业规划知识库归一化助手。
                    任务：把一个岗位画像归一化到候选岗位族之一。
                    必须只从候选列表中选 1 个，不能自造新类目。
                    如果拿不准，也要选最接近的那个。
                    只输出 JSON：
                    {"role_family":"候选中的一个"}

                    [候选岗位族]
                    %s

                    [岗位标题]
                    %s

                    [岗位类别]
                    %s

                    [岗位技能]
                    %s

                    [岗位描述]
                    %s
                    """.formatted(
                    String.join(" | ", candidates),
                    safe(job.title()),
                    safe(job.category()),
                    String.join(" | ", safeList(job.requiredSkills())),
                    safe(job.description())
            );
            String family = readStringField(callJson(prompt), "role_family");
            return candidates.stream()
                    .filter(candidate -> normalize(candidate).equals(normalize(family)))
                    .findFirst()
                    .orElse("");
        });
    }

    public String resolveProfileIntent(StudentProfileDTO studentProfile, List<String> candidates) {
        if (studentProfile == null || candidates == null || candidates.isEmpty()) {
            return "";
        }
        String cacheKey = "profile|" + safe(studentProfile.jobPreference() == null ? null : studentProfile.jobPreference().expectedPosition())
                + "|" + String.join("|", safeList(studentProfile.skills()));
        return familyCache.computeIfAbsent(cacheKey, key -> {
            String prompt = """
                    你是职业规划意图归一化助手。
                    任务：把学生的求职意向归一化到候选岗位族之一。
                    必须只从候选列表中选 1 个，不能自造新类目。
                    只输出 JSON：
                    {"role_family":"候选中的一个"}

                    [候选岗位族]
                    %s

                    [学生期望岗位]
                    %s

                    [学生技能]
                    %s

                    [学生专业]
                    %s
                    """.formatted(
                    String.join(" | ", candidates),
                    safe(studentProfile.jobPreference() == null ? null : studentProfile.jobPreference().expectedPosition()),
                    String.join(" | ", safeList(studentProfile.skills())),
                    safe(studentProfile.basicInfo() == null ? null : studentProfile.basicInfo().major())
            );
            String family = readStringField(callJson(prompt), "role_family");
            return candidates.stream()
                    .filter(candidate -> normalize(candidate).equals(normalize(family)))
                    .findFirst()
                    .orElse("");
        });
    }

    public List<String> refineConcreteSkills(JobProfileDTO job) {
        if (job == null) {
            return List.of();
        }
        String cacheKey = "skills|" + safe(job.title()) + "|" + String.join("|", safeList(job.requiredSkills()));
        return skillCache.computeIfAbsent(cacheKey, key -> {
            String prompt = """
                    你是岗位技能去泛化助手。
                    任务：把岗位画像中的泛化技能词整理成具体、可执行、可学习的技能点。
                    要求：
                    1. 保留具体技术名词，如 Java、Spring Boot、MySQL、Redis、Vue、Postman。
                    2. 删除过泛词，如 编程语言、数据库、前端框架、消息队列、设计模式、专业技能。
                    3. 最多输出 8 个。
                    4. 只输出 JSON：
                    {"concrete_skills":["技能1","技能2"]}

                    [岗位标题]
                    %s

                    [原始技能]
                    %s

                    [岗位描述]
                    %s
                    """.formatted(
                    safe(job.title()),
                    String.join(" | ", safeList(job.requiredSkills())),
                    safe(job.description())
            );

            JsonNode root = callJson(prompt);
            if (root == null || !root.path("concrete_skills").isArray()) {
                return List.of();
            }

            List<String> skills = new ArrayList<>();
            root.path("concrete_skills").forEach(item -> {
                String value = item.asText("").trim();
                if (!value.isBlank()) {
                    skills.add(value);
                }
            });
            return skills.stream().distinct().limit(8).toList();
        });
    }

    private JsonNode callJson(String prompt) {
        try {
            String content = CompletableFuture.supplyAsync(() -> pureChatClient.prompt()
                            .system("你必须严格输出 JSON，对象外不要有任何解释文字。")
                            .user(prompt)
                            .options(ChatOptions.builder().temperature(0.1).build())
                            .call()
                            .content())
                    .get(10, TimeUnit.SECONDS);
            if (content == null || content.isBlank()) {
                return null;
            }
            return objectMapper.readTree(content);
        } catch (Exception e) {
            return null;
        }
    }

    private String readStringField(JsonNode root, String field) {
        return root == null ? "" : root.path(field).asText("");
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").replace("/", "").replace("+", "");
    }
}
