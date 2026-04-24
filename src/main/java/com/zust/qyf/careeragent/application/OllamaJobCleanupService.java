package com.zust.qyf.careeragent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.NormalizedJobRecordEntity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OllamaJobCleanupService {
    private static final List<String> ALLOWED_CATEGORIES = List.of(
            "Java开发", "前端开发", "Python开发", "算法工程师", "数据分析",
            "软件测试", "硬件测试", "实施工程师", "技术支持工程师",
            "运维/DevOps", "产品/项目经理", "APP推广/运营", "科研人员",
            "综合开发", "非信息化岗位"
    );

    private static final Pattern CITY_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,8})(?:市)?(?:[-/路 ]?([\\u4e00-\\u9fa5]{1,12}(?:区|县|镇|新区|开发区)))?");
    private static final Pattern COMPANY_SIZE_PATTERN = Pattern.compile("(\\d{1,5}-\\d{1,5}人|\\d{1,5}人以上|少于\\d{1,5}人)");
    private static final Pattern JOB_CODE_PATTERN = Pattern.compile("(CC[A-Z0-9]+|[A-Z]{2,6}\\d{6,})");
    private static final Pattern EDUCATION_PATTERN = Pattern.compile("(博士|硕士|研究生|本科|大专|专科|中专|高中)(?:及以上|以上|及以下|以下)?");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d+\\s*(?:-|~|到|至)\\s*\\d+\\s*年|\\d+\\s*年以上|应届(?:毕业生)?|实习(?:生)?|无经验|经验不限)");

    private final ChatClient ollamaChatClient;
    private final ObjectMapper objectMapper;

    @Value("${app.job-cleanup.llm-enabled:true}")
    private boolean llmEnabled;

    public OllamaJobCleanupService(@Qualifier("ollamaChatClient") ChatClient ollamaChatClient,
                                   ObjectMapper objectMapper) {
        this.ollamaChatClient = ollamaChatClient;
        this.objectMapper = objectMapper;
    }

    public NormalizedJobRecordEntity cleanupIfNeeded(NormalizedJobRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        NormalizedJobRecordEntity working = entity;
        if (llmEnabled) {
            try {
                CleanupPayload payload = requestOllamaCleanup(entity);
                if (payload != null) {
                    applyPayload(working, payload);
                }
            } catch (Exception ignored) {
                // LLM cleanup is primary, but import should still complete with fallback normalization.
            }
        }

        fillFallbackFields(working);
        normalizeFinalState(working);
        return working;
    }

    private CleanupPayload requestOllamaCleanup(NormalizedJobRecordEntity entity) throws Exception {
        String schema = """
                {
                  "company_name": "",
                  "job_title": "",
                  "job_category": "",
                  "work_address": "",
                  "city": "",
                  "salary_text": "",
                  "education_level": "",
                  "experience_text": "",
                  "job_description": "",
                  "company_intro": "",
                  "skills": [],
                  "required_certificates": [],
                  "is_valid": true,
                  "invalid_reason": "",
                  "confidence_score": 0.0
                }
                """;

        String userPrompt = """
                Clean this job posting into structured JSON.

                Allowed categories:
                %s

                Rules:
                1. Extract explicit facts only. Do not guess.
                2. job_description must focus on responsibilities and requirements.
                3. Remove company marketing, benefits, timestamps, and tracking params from job_description.
                4. company_intro should keep only brief company background.
                5. skills must contain only explicit technical/domain skills, max 8.
                6. required_certificates should contain only explicit certificates. If none, return ["无"].
                7. If the role is clearly non-digital, set job_category to "非信息化岗位", is_valid=false, invalid_reason="non_digital_job".
                8. confidence_score must be between 0 and 1.
                9. Return JSON only.

                Raw fields:
                source_id: %s
                company_name: %s
                job_title: %s
                industry: %s
                work_address: %s
                salary_text: %s
                education_level: %s
                experience_text: %s
                company_intro: %s
                job_description: %s
                source_url: %s

                Output schema:
                %s
                """.formatted(
                String.join(", ", ALLOWED_CATEGORIES),
                safe(entity.getSourceId()),
                safe(entity.getCompanyName()),
                safe(entity.getJobTitle()),
                safe(entity.getIndustry()),
                safe(entity.getWorkAddress()),
                safe(entity.getSalaryText()),
                safe(entity.getEducationLevel()),
                safe(entity.getExperienceText()),
                safe(entity.getCompanyIntro()),
                safe(entity.getJobDescription()),
                safe(entity.getSourceUrl()),
                schema
        );

        String response = ollamaChatClient.prompt()
                .system("You are a strict job-posting data cleaning agent. Return only valid JSON.")
                .user(userPrompt)
                .call()
                .content();

        if (response == null || response.isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(extractJsonObject(response));
        CleanupPayload payload = new CleanupPayload();
        payload.companyName = text(root, "company_name");
        payload.jobTitle = text(root, "job_title");
        payload.jobCategory = text(root, "job_category");
        payload.workAddress = text(root, "work_address");
        payload.city = text(root, "city");
        payload.salaryText = text(root, "salary_text");
        payload.educationLevel = text(root, "education_level");
        payload.experienceText = text(root, "experience_text");
        payload.jobDescription = text(root, "job_description");
        payload.companyIntro = text(root, "company_intro");
        payload.skills = array(root, "skills");
        payload.requiredCertificates = array(root, "required_certificates");
        payload.isValid = root.path("is_valid").asBoolean(true);
        payload.invalidReason = text(root, "invalid_reason");
        payload.confidenceScore = root.path("confidence_score").asDouble(0.0);
        return payload;
    }

    private void applyPayload(NormalizedJobRecordEntity entity, CleanupPayload payload) {
        if (!blank(payload.companyName)) entity.setCompanyName(payload.companyName);
        if (!blank(payload.jobTitle)) entity.setJobTitle(payload.jobTitle);
        if (!blank(payload.jobCategory)) entity.setJobCategory(normalizeCategory(payload.jobCategory));
        if (!blank(payload.workAddress)) entity.setWorkAddress(payload.workAddress);
        if (!blank(payload.city)) entity.setCity(payload.city);
        if (!blank(payload.salaryText)) entity.setSalaryText(payload.salaryText);
        if (!blank(payload.educationLevel)) entity.setEducationLevel(payload.educationLevel);
        if (!blank(payload.experienceText)) entity.setExperienceText(payload.experienceText);
        if (!blank(payload.jobDescription)) entity.setJobDescription(payload.jobDescription);
        if (!blank(payload.companyIntro)) entity.setCompanyIntro(payload.companyIntro);
        if (!payload.skills.isEmpty()) entity.setSkills(cleanList(payload.skills));
        if (!payload.requiredCertificates.isEmpty()) entity.setRequiredCertificates(cleanList(payload.requiredCertificates));
        entity.setIsValid(payload.isValid);
        entity.setInvalidReason(blank(payload.invalidReason) ? "" : payload.invalidReason);
        entity.setConfidenceScore(Math.max(0.0, Math.min(1.0, payload.confidenceScore)));
    }

    private void fillFallbackFields(NormalizedJobRecordEntity entity) {
        String raw = mergedText(entity);
        if (blank(entity.getWorkAddress())) {
            entity.setWorkAddress(extractAddress(raw));
        }
        if (blank(entity.getCity())) {
            entity.setCity(extractCity(firstNonBlank(entity.getWorkAddress(), raw)));
        }
        if (blank(entity.getCompanySize())) {
            entity.setCompanySize(extractCompanySize(raw));
        }
        if (blank(entity.getJobCode())) {
            entity.setJobCode(extractJobCode(entity.getSourceUrl(), raw));
        }
        if (blank(entity.getEducationLevel())) {
            entity.setEducationLevel(extractEducation(raw));
        }
        if (blank(entity.getExperienceText())) {
            entity.setExperienceText(extractExperience(raw));
        }
        if (entity.getSkills() == null || entity.getSkills().isEmpty()) {
            entity.setSkills(extractRuleSkills(raw));
        }
        if (entity.getRequiredCertificates() == null || entity.getRequiredCertificates().isEmpty()) {
            entity.setRequiredCertificates(List.of("无"));
        }
        if (blank(entity.getJobCategory())) {
            entity.setJobCategory(normalizeCategory(inferCategoryFallback(entity)));
        }
        if (blank(entity.getJobDescription())) {
            entity.setJobDescription(buildShortDescription(raw));
        }
        if (blank(entity.getCompanyIntro())) {
            entity.setCompanyIntro(extractCompanyIntro(raw, entity.getCompanyName()));
        }
    }

    private void normalizeFinalState(NormalizedJobRecordEntity entity) {
        entity.setJobCategory(normalizeCategory(entity.getJobCategory()));
        entity.setSkills(cleanList(entity.getSkills()));
        entity.setRequiredCertificates(cleanList(entity.getRequiredCertificates()).isEmpty()
                ? List.of("无")
                : cleanList(entity.getRequiredCertificates()));
        entity.setJobDescription(cleanText(entity.getJobDescription()));
        entity.setCompanyIntro(cleanText(entity.getCompanyIntro()));

        boolean valid = !"非信息化岗位".equals(entity.getJobCategory())
                && !blank(entity.getCompanyName())
                && !blank(entity.getJobTitle())
                && !blank(entity.getJobDescription());
        entity.setIsValid(valid);
        if (!valid && blank(entity.getInvalidReason())) {
            entity.setInvalidReason("non_digital_job");
        }
        entity.setStatus(valid ? "active" : "rejected");
        entity.setCleanStatus("ready_llm");
        entity.setCleanSource(llmEnabled ? "ollama_primary_v1" : "rule_fallback_v1");
        if (entity.getConfidenceScore() == null || entity.getConfidenceScore() <= 0) {
            entity.setConfidenceScore(valid ? 0.65 : 0.35);
        }
    }

    private String mergedText(NormalizedJobRecordEntity entity) {
        return String.join(" ",
                safe(entity.getCompanyName()),
                safe(entity.getJobTitle()),
                safe(entity.getIndustry()),
                safe(entity.getWorkAddress()),
                safe(entity.getSalaryText()),
                safe(entity.getEducationLevel()),
                safe(entity.getExperienceText()),
                safe(entity.getJobDescription()),
                safe(entity.getCompanyIntro()),
                safe(entity.getSourceUrl())
        );
    }

    private String extractAddress(String text) {
        Matcher matcher = CITY_PATTERN.matcher(safe(text));
        if (!matcher.find()) {
            return "";
        }
        String city = matcher.group(1);
        String district = matcher.group(2);
        return blank(district) ? city : city + "-" + district;
    }

    private String extractCity(String text) {
        Matcher matcher = CITY_PATTERN.matcher(safe(text));
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractCompanySize(String text) {
        Matcher matcher = COMPANY_SIZE_PATTERN.matcher(safe(text));
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractJobCode(String sourceUrl, String text) {
        Matcher matcher = JOB_CODE_PATTERN.matcher(firstNonBlank(sourceUrl, text));
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractEducation(String text) {
        Matcher matcher = EDUCATION_PATTERN.matcher(safe(text));
        if (!matcher.find()) {
            return "";
        }
        return switch (matcher.group(1)) {
            case "研究生" -> "硕士";
            case "专科" -> "大专";
            default -> matcher.group(1);
        };
    }

    private String extractExperience(String text) {
        Matcher matcher = EXPERIENCE_PATTERN.matcher(safe(text));
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", "") : "";
    }

    private List<String> extractRuleSkills(String text) {
        Set<String> values = new LinkedHashSet<>();
        String haystack = safe(text).toLowerCase(Locale.ROOT);
        for (String keyword : List.of("java", "spring", "python", "django", "flask", "fastapi", "vue", "react",
                "javascript", "typescript", "html", "css", "mysql", "oracle", "sql", "docker", "kubernetes",
                "selenium", "jmeter", "postman", "arcgis", "gis", "erp", "mes", "wms", "plm", "linux", "c++", "qt")) {
            if (haystack.contains(keyword)) {
                values.add(keyword.equals("c++") ? "C++" : keyword.substring(0, 1).toUpperCase(Locale.ROOT) + keyword.substring(1));
            }
        }
        return new ArrayList<>(values);
    }

    private String inferCategoryFallback(NormalizedJobRecordEntity entity) {
        String title = safe(entity.getJobTitle()).toLowerCase(Locale.ROOT);
        String text = mergedText(entity).toLowerCase(Locale.ROOT);
        if (containsAny(title, "实施", "交付")) return "实施工程师";
        if (containsAny(title, "技术支持", "售后", "客服")) return "技术支持工程师";
        if (containsAny(title, "测试")) return containsAny(text, "硬件", "板卡", "芯片") ? "硬件测试" : "软件测试";
        if (containsAny(title, "前端")) return "前端开发";
        if (containsAny(title, "java")) return "Java开发";
        if (containsAny(title, "python")) return "Python开发";
        if (containsAny(title, "算法")) return "算法工程师";
        if (containsAny(title, "数据")) return "数据分析";
        if (containsAny(title, "运维", "devops", "sre")) return "运维/DevOps";
        if (containsAny(title, "产品经理", "项目经理")) return "产品/项目经理";
        if (containsAny(title, "运营", "推广", "投放", "增长")) return "APP推广/运营";
        if (containsAny(title, "科研", "研究")) return containsAny(text, "计算机", "软件", "人工智能", "数据", "算法") ? "科研人员" : "非信息化岗位";
        if (containsAny(text, "java", "python", "vue", "react", "spring", "mysql", "oracle", "sql", "docker", "kubernetes")) {
            return "综合开发";
        }
        return "非信息化岗位";
    }

    private String buildShortDescription(String text) {
        String cleaned = cleanText(safe(text));
        if (cleaned.length() <= 400) {
            return cleaned;
        }
        return cleaned.substring(0, 400);
    }

    private String extractCompanyIntro(String text, String companyName) {
        String cleaned = cleanText(safe(text));
        if (blank(cleaned)) {
            return "";
        }
        if (!blank(companyName) && cleaned.contains(companyName)) {
            int start = cleaned.indexOf(companyName);
            int end = Math.min(cleaned.length(), start + 160);
            return cleaned.substring(start, end);
        }
        return cleaned.length() <= 160 ? cleaned : cleaned.substring(0, 160);
    }

    private List<String> cleanList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> !blank(value))
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList();
    }

    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("No JSON object found in Ollama response");
        }
        return content.substring(start, end + 1);
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isMissingNode() || node.isNull() ? "" : node.asText("");
    }

    private List<String> array(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("");
            if (!blank(value)) {
                values.add(value.trim());
            }
        }
        return values;
    }

    private String normalizeCategory(String value) {
        if (blank(value)) {
            return "综合开发";
        }
        if (ALLOWED_CATEGORIES.contains(value)) {
            return value;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "frontend" -> "前端开发";
            case "backend" -> "Java开发";
            case "testing" -> "软件测试";
            case "data" -> "数据分析";
            case "other" -> "综合开发";
            default -> value;
        };
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!blank(value)) {
                return value;
            }
        }
        return "";
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("<br>", "；")
                .replace("<br/>", "；")
                .replace("<br />", "；")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace('\u3000', ' ')
                .replaceAll("[\\p{Cc}\\p{Cf}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static final class CleanupPayload {
        private String companyName = "";
        private String jobTitle = "";
        private String jobCategory = "";
        private String workAddress = "";
        private String city = "";
        private String salaryText = "";
        private String educationLevel = "";
        private String experienceText = "";
        private String jobDescription = "";
        private String companyIntro = "";
        private List<String> skills = List.of();
        private List<String> requiredCertificates = List.of();
        private boolean isValid = true;
        private String invalidReason = "";
        private double confidenceScore = 0.0;
    }
}
