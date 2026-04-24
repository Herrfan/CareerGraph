package com.zust.qyf.careeragent.application;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class JobPortraitAssembler {
    public Map<String, String> buildAbilityPortrait(String category,
                                                    List<String> skills,
                                                    List<String> certificates,
                                                    String experienceRequired,
                                                    String description) {
        String normalizedText = normalize(category) + " " + normalize(experienceRequired) + " " + normalize(description);
        String skillSummary = joinOrDefault(skills, "当前样本未提取到稳定技术栈");
        String certificateSummary = normalizeCertificates(certificates);

        Map<String, String> portrait = new LinkedHashMap<>();
        portrait.put("job_overview", buildJobOverviewSummary(category, experienceRequired, skillSummary));
        portrait.put("job_responsibilities", buildJobResponsibilitiesSummary(description));
        portrait.put("professional_skills", buildProfessionalSkillsSummary(skillSummary, normalizedText));
        portrait.put("certificates", buildCertificateSummary(certificateSummary, normalizedText));
        portrait.put("innovation", buildInnovationSummary(category, normalizedText));
        portrait.put("learning", buildLearningSummary(category, skillSummary, normalizedText));
        portrait.put("stress_tolerance", buildStressSummary(category, normalizedText));
        portrait.put("communication", buildCommunicationSummary(category, normalizedText));
        portrait.put("internship", buildInternshipSummary(experienceRequired, normalizedText));
        portrait.put("teamwork", buildTeamworkSummary(category, normalizedText));
        portrait.put("execution", buildExecutionSummary(category, normalizedText));
        portrait.put("problem_solving", buildProblemSolvingSummary(category, normalizedText));
        portrait.put("responsibility", buildResponsibilitySummary(category, normalizedText));
        return portrait;
    }

    public Map<String, Integer> buildAbilityPriority(String category,
                                                     List<String> skills,
                                                     List<String> certificates,
                                                     String experienceRequired,
                                                     String description) {
        String normalizedText = normalize(category) + " " + normalize(experienceRequired) + " " + normalize(description);
        boolean technicalHeavy = isTechnicalCategory(category) || (skills != null && !skills.isEmpty());
        boolean customerFacing = containsAny(normalizedText, "客户", "交付", "培训", "需求", "汇报", "协作", "沟通");
        boolean pressureHeavy = containsAny(normalizedText, "上线", "交付", "故障", "响应", "并行", "推进", "值班");
        boolean innovationHeavy = containsAny(normalizedText, "设计", "优化", "架构", "创新", "研究", "方案");
        boolean internshipHeavy = containsAny(normalizedText, "应届", "实习", "校招", "项目", "实践");
        boolean teamworkHeavy = customerFacing || containsAny(normalizedText, "团队", "跨部门", "协同", "配合");
        boolean executionHeavy = containsAny(normalizedText, "跟进", "推进", "落地", "执行", "交付", "完成");
        boolean problemHeavy = containsAny(normalizedText, "排查", "定位", "分析", "解决", "优化", "调试");

        Map<String, Integer> priority = new LinkedHashMap<>();
        priority.put("professional_skills", technicalHeavy ? 92 : 74);
        priority.put("certificates", certificates != null && !certificates.isEmpty() && !List.of("无").equals(certificates) ? 74 : 48);
        priority.put("innovation", innovationHeavy || "算法工程师".equals(category) || "科研人员".equals(category) ? 78 : 60);
        priority.put("learning", technicalHeavy ? 80 : 68);
        priority.put("stress_tolerance", pressureHeavy ? 78 : 62);
        priority.put("communication", customerFacing || "产品/项目经理".equals(category) || "技术支持工程师".equals(category) || "实施工程师".equals(category) ? 84 : 66);
        priority.put("internship", internshipHeavy ? 82 : 70);
        priority.put("teamwork", teamworkHeavy ? 82 : 68);
        priority.put("execution", executionHeavy ? 84 : 70);
        priority.put("problem_solving", problemHeavy || technicalHeavy ? 86 : 68);
        priority.put("responsibility", pressureHeavy || executionHeavy ? 82 : 70);
        return priority;
    }

    public String appendPortraitMarkdown(String baseMarkdown,
                                         String category,
                                         List<String> skills,
                                         List<String> certificates,
                                         String experienceRequired,
                                         String description) {
        Map<String, String> portrait = buildAbilityPortrait(category, skills, certificates, experienceRequired, description);
        Map<String, Integer> priority = buildAbilityPriority(category, skills, certificates, experienceRequired, description);
        StringBuilder builder = new StringBuilder(baseMarkdown == null ? "" : baseMarkdown.trim());
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append("## 岗位能力画像\n");
        for (Map.Entry<String, String> entry : portrait.entrySet()) {
            builder.append("- ").append(label(entry.getKey()))
                    .append("（重要度 ").append(priority.getOrDefault(entry.getKey(), 60)).append("）")
                    .append("：").append(entry.getValue())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    public Map<String, Double> buildSkillVector(List<String> skills, List<String> certificates) {
        LinkedHashMap<String, Double> vector = new LinkedHashMap<>();
        int index = 0;
        for (String skill : skills == null ? List.<String>of() : skills) {
            String key = normalizeVectorKey(skill);
            if (key.isBlank() || vector.containsKey(key)) {
                continue;
            }
            vector.put(key, roundWeight(Math.max(0.2, 1.0 - index * 0.08)));
            index++;
        }

        int certificateIndex = 0;
        for (String certificate : certificates == null ? List.<String>of() : certificates) {
            if (certificate == null || certificate.isBlank() || "无".equals(certificate)) {
                continue;
            }
            String key = "cert_" + normalizeVectorKey(certificate);
            if (key.isBlank() || vector.containsKey(key)) {
                continue;
            }
            vector.put(key, roundWeight(Math.max(0.1, 0.35 - certificateIndex * 0.05)));
            certificateIndex++;
        }

        if (vector.isEmpty()) {
            vector.put("generic_signal", 0.1);
        }
        return vector;
    }

    private String buildJobOverviewSummary(String category, String experienceRequired, String skillSummary) {
        String safeCategory = (category == null || category.isBlank()) ? "综合方向" : category;
        String safeExperience = (experienceRequired == null || experienceRequired.isBlank()) ? "未标注" : experienceRequired;
        if (!"当前样本未提取到稳定技术栈".equals(skillSummary)) {
            return "岗位方向为" + safeCategory + "，经验要求" + safeExperience + "，岗位文本提取到 " + skillSummary + "。";
        }
        return "岗位方向为" + safeCategory + "，经验要求" + safeExperience + "，当前样本未提取到稳定技术栈。";
    }

    private String buildJobResponsibilitiesSummary(String description) {
        if (description == null || description.isBlank()) {
            return "主要职责围绕需求理解、任务执行与结果交付。";
        }
        String[] segments = description.replace('\r', '\n').split("[。；;\\n]");
        for (String segment : segments) {
            String compact = compactText(segment);
            if (compact.isBlank()) {
                continue;
            }
            if (containsAny(compact, "负责", "参与", "推进", "交付", "开发", "测试", "设计", "维护", "排查")) {
                return "主要职责：" + limitLength(compact, 90) + (compact.endsWith("。") ? "" : "。");
            }
        }
        String fallback = limitLength(compactText(description), 90);
        return "主要职责：" + fallback + (fallback.endsWith("。") ? "" : "。");
    }

    private String buildProfessionalSkillsSummary(String skillSummary, String normalizedText) {
        if (!"当前样本未提取到稳定技术栈".equals(skillSummary)) {
            return "岗位文本提取到 " + skillSummary + "，说明专业技能是该岗位画像中的核心维度。";
        }
        if (containsAny(normalizedText, "系统", "平台", "开发", "测试", "数据", "部署")) {
            return "岗位职责明显偏向系统实现、测试交付或平台维护，专业技能仍然是核心要求，但原始样本还需要继续补齐技术细节。";
        }
        return "当前岗位样本缺少稳定技术栈信息，建议继续结合原始 JD 补全专业技能要求。";
    }

    private String buildCertificateSummary(String certificateSummary, String normalizedText) {
        if (!"无".equals(certificateSummary)) {
            return "岗位文本提取到 " + certificateSummary + "，证书或资质属于明确要求或高价值加分项。";
        }
        if (containsAny(normalizedText, "资质", "认证", "资格证", "证书")) {
            return "岗位文本出现了证书相关信号，但当前样本未能稳定抽取到具体名称，建议人工复核原始 JD。";
        }
        return "当前岗位样本未提取到硬性证书要求，通常更看重技能、项目和实践证据。";
    }

    private String buildInnovationSummary(String category, String normalizedText) {
        if (containsAny(normalizedText, "架构", "优化", "方案设计", "创新", "研究", "探索")) {
            return "岗位职责中包含方案设计、优化或研究任务，说明该岗位对创新能力有中高要求。";
        }
        if ("算法工程师".equals(category) || "科研人员".equals(category)) {
            return "岗位类型本身偏向研究和问题探索，创新能力是重要画像维度。";
        }
        return "该岗位更强调在现有业务场景中持续改进与问题拆解，创新能力属于中等要求。";
    }

    private String buildLearningSummary(String category, String skillSummary, String normalizedText) {
        if (containsAny(normalizedText, "新技术", "持续学习", "快速学习", "迭代", "复杂")) {
            return "岗位文本强调持续学习和快速适应，说明学习能力要求较高。";
        }
        if (isTechnicalCategory(category) && !"当前样本未提取到稳定技术栈".equals(skillSummary) && skillSummary.contains("、")) {
            return "岗位涉及多项技术栈协同，说明候选人需要具备持续学习和快速补齐能力。";
        }
        return "岗位需要稳定吸收业务和工具知识，学习能力属于基础且长期有效的画像维度。";
    }

    private String buildStressSummary(String category, String normalizedText) {
        if (containsAny(normalizedText, "上线", "交付", "故障", "高并发", "响应", "排查", "多任务")) {
            return "岗位职责涉及上线、排障或多任务推进，抗压能力要求偏高。";
        }
        if ("实施工程师".equals(category) || "技术支持工程师".equals(category) || "运维/DevOps".equals(category)) {
            return "岗位类型决定了需要面对交付节奏、客户响应或线上问题，抗压能力是重要维度。";
        }
        return "岗位整体节奏偏工程执行，抗压能力属于中等要求，但仍需要稳定推进任务。";
    }

    private String buildCommunicationSummary(String category, String normalizedText) {
        if (containsAny(normalizedText, "客户", "需求", "汇报", "培训", "协作", "沟通", "跨部门")) {
            return "岗位文本出现客户、需求或协作场景，说明沟通能力是明确要求。";
        }
        if ("产品/项目经理".equals(category) || "实施工程师".equals(category) || "技术支持工程师".equals(category)) {
            return "该岗位需要持续协调业务、客户或团队成员，沟通能力要求较高。";
        }
        return "岗位虽然以执行为主，但仍需要完成团队协作与结果对齐，沟通能力属于基础要求。";
    }

    private String buildInternshipSummary(String experienceRequired, String normalizedText) {
        if (containsAny(normalize(experienceRequired), "实习", "应届", "校招")) {
            return "岗位面向应届或实习候选人时，更看重项目实战和实习可验证成果。";
        }
        if (containsAny(normalizedText, "项目", "实践", "落地", "交付", "现场")) {
            return "岗位职责明显依赖项目落地或实践经验，实习/实践能力是重要画像维度。";
        }
        return "即使岗位未明确写出实习要求，也需要通过课程项目、实训或实践经历证明可上手能力。";
    }

    private String buildTeamworkSummary(String category, String normalizedText) {
        if (containsAny(normalizedText, "团队", "协同", "跨部门", "配合", "协作")) {
            return "岗位职责明确包含协同推进、跨角色配合或团队交付，团队协作属于高频要求。";
        }
        if ("产品/项目经理".equals(category) || "实施工程师".equals(category) || "技术支持工程师".equals(category)) {
            return "该岗位天然需要和研发、客户或业务多方协作，团队协作能力要求较高。";
        }
        return "岗位以工程执行为主，但仍需要在团队上下游中稳定配合，团队协作属于基础要求。";
    }

    private String buildExecutionSummary(String category, String normalizedText) {
        if (containsAny(normalizedText, "推进", "执行", "落地", "交付", "跟进", "完成")) {
            return "岗位文本明确强调任务推进、落地执行或交付闭环，执行推进能力要求较高。";
        }
        if ("实施工程师".equals(category) || "运维/DevOps".equals(category) || "技术支持工程师".equals(category)) {
            return "岗位类型本身强调执行闭环和结果交付，执行推进能力是关键画像维度。";
        }
        return "该岗位仍需要把方案真正落到结果上，执行推进能力属于中等偏上的要求。";
    }

    private String buildProblemSolvingSummary(String category, String normalizedText) {
        if (containsAny(normalizedText, "分析", "排查", "定位", "解决", "调试", "优化")) {
            return "岗位职责中包含问题定位、分析和解决任务，说明问题分析与解决能力要求较高。";
        }
        if (isTechnicalCategory(category)) {
            return "技术类岗位需要持续处理缺陷、异常或性能问题，问题分析与解决能力是核心维度。";
        }
        return "岗位虽不完全以技术攻关为主，但仍需要具备基本的问题判断与处理能力。";
    }

    private String buildResponsibilitySummary(String category, String normalizedText) {
        if (containsAny(normalizedText, "负责", "独立", "跟进", "结果", "闭环", "责任")) {
            return "岗位文本强调独立负责、结果闭环或责任落实，责任意识要求较高。";
        }
        if ("产品/项目经理".equals(category) || "实施工程师".equals(category) || "技术支持工程师".equals(category)) {
            return "该岗位往往直接面对业务结果或客户反馈，责任意识是高优先级维度。";
        }
        return "岗位整体更偏执行岗位，但仍需要对任务质量、时间和结果保持稳定负责。";
    }

    private String normalizeCertificates(List<String> certificates) {
        if (certificates == null || certificates.isEmpty()) {
            return "无";
        }
        List<String> cleaned = certificates.stream()
                .filter(value -> value != null && !value.isBlank() && !"无".equals(value))
                .distinct()
                .toList();
        return cleaned.isEmpty() ? "无" : joinOrDefault(cleaned, "无");
    }

    private String label(String key) {
        return switch (key) {
            case "job_overview" -> "岗位概览";
            case "job_responsibilities" -> "岗位职责";
            case "professional_skills" -> "专业技能";
            case "certificates" -> "证书要求";
            case "innovation" -> "创新能力";
            case "learning" -> "学习能力";
            case "stress_tolerance" -> "抗压能力";
            case "communication" -> "沟通能力";
            case "internship" -> "实习/实践能力";
            case "teamwork" -> "团队协作";
            case "execution" -> "执行推进";
            case "problem_solving" -> "问题分析与解决";
            case "responsibility" -> "责任意识";
            default -> key;
        };
    }

    private boolean isTechnicalCategory(String category) {
        return List.of("Java开发", "前端开发", "Python开发", "C/C++开发", "算法工程师", "数据分析", "软件测试", "硬件测试", "运维/DevOps", "综合开发")
                .contains(category);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT)) || text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String joinOrDefault(List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : String.join("、", values);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String compactText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String limitLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String normalizeVectorKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", "_");
    }

    private double roundWeight(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
