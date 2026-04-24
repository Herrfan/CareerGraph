package com.zust.qyf.careeragent.domain.service;

import com.zust.qyf.careeragent.application.CareerAiDecisionService;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.match.CategoryMatchDTO;
import com.zust.qyf.careeragent.domain.dto.match.DimensionScoreDTO;
import com.zust.qyf.careeragent.domain.dto.match.JobMatchDTO;
import com.zust.qyf.careeragent.domain.dto.match.MatchResultDTO;
import com.zust.qyf.careeragent.domain.dto.student.JobPreferenceDTO;
import com.zust.qyf.careeragent.domain.dto.student.SoftAbilitiesDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchScoringService {
    private static final String CATEGORY_JAVA = "Java开发";
    private static final String CATEGORY_FRONTEND = "前端开发";
    private static final String CATEGORY_PYTHON = "Python开发";
    private static final String CATEGORY_ALGORITHM = "算法工程师";
    private static final String CATEGORY_DATA = "数据分析";
    private static final String CATEGORY_SOFTWARE_TEST = "软件测试";
    private static final String CATEGORY_HARDWARE_TEST = "硬件测试";
    private static final String CATEGORY_IMPLEMENTATION = "实施工程师";
    private static final String CATEGORY_SUPPORT = "技术支持工程师";
    private static final String CATEGORY_DEVOPS = "运维/DevOps";
    private static final String CATEGORY_PM = "产品/项目经理";
    private static final String CATEGORY_OPERATIONS = "APP推广/运营";
    private static final String CATEGORY_RESEARCH = "科研人员";
    private static final String CATEGORY_GENERAL = "综合开发";

    private static final Map<String, Map<String, Double>> CATEGORY_WEIGHTS = Map.ofEntries(
            Map.entry(CATEGORY_JAVA, weights(0.58, 0.26, 0.06, 0.10)),
            Map.entry(CATEGORY_FRONTEND, weights(0.58, 0.26, 0.06, 0.10)),
            Map.entry(CATEGORY_PYTHON, weights(0.57, 0.26, 0.07, 0.10)),
            Map.entry(CATEGORY_ALGORITHM, weights(0.54, 0.30, 0.06, 0.10)),
            Map.entry(CATEGORY_DATA, weights(0.56, 0.26, 0.08, 0.10)),
            Map.entry(CATEGORY_SOFTWARE_TEST, weights(0.56, 0.28, 0.06, 0.10)),
            Map.entry(CATEGORY_HARDWARE_TEST, weights(0.56, 0.28, 0.06, 0.10)),
            Map.entry(CATEGORY_IMPLEMENTATION, weights(0.60, 0.22, 0.08, 0.10)),
            Map.entry(CATEGORY_SUPPORT, weights(0.60, 0.20, 0.10, 0.10)),
            Map.entry(CATEGORY_DEVOPS, weights(0.57, 0.25, 0.08, 0.10)),
            Map.entry(CATEGORY_PM, weights(0.60, 0.18, 0.12, 0.10)),
            Map.entry(CATEGORY_OPERATIONS, weights(0.62, 0.16, 0.12, 0.10)),
            Map.entry(CATEGORY_RESEARCH, weights(0.54, 0.28, 0.08, 0.10)),
            Map.entry(CATEGORY_GENERAL, weights(0.58, 0.24, 0.08, 0.10))
    );
    private final CareerAiDecisionService careerAiDecisionService;

    public MatchScoringService(CareerAiDecisionService careerAiDecisionService) {
        this.careerAiDecisionService = careerAiDecisionService;
    }

    public MatchResultDTO calculateMatch(StudentProfileDTO studentProfile, JobProfileDTO jobProfile) {
        DimensionScoreDTO dimensionScores = calculateDimensionScores(studentProfile, jobProfile);
        Map<String, Double> weights = CATEGORY_WEIGHTS.getOrDefault(resolveWeightCategory(jobProfile), CATEGORY_WEIGHTS.get(CATEGORY_GENERAL));
        double totalScore = dimensionScores.basicRequirements() * weights.get("basic")
                + dimensionScores.professionalSkills() * weights.get("skills")
                + dimensionScores.professionalQuality() * weights.get("quality")
                + dimensionScores.growthPotential() * weights.get("growth")
                + calculateTargetPreferenceBonus(studentProfile, jobProfile);

        return new MatchResultDTO(
                round(clamp(totalScore)),
                dimensionScores,
                jobProfile,
                buildRecommendations(studentProfile, jobProfile, dimensionScores)
        );
    }

    public List<JobMatchDTO> calculateTopMatches(StudentProfileDTO studentProfile, List<JobProfileDTO> jobs, int topN) {
        return jobs.stream()
                .map(job -> {
                    MatchResultDTO result = calculateMatch(studentProfile, job);
                    return new JobMatchDTO(job, result.matchScore(), result.dimensionScores());
                })
                .sorted(Comparator.comparingDouble(JobMatchDTO::matchScore).reversed())
                .limit(Math.max(topN, 1))
                .toList();
    }

    public List<CategoryMatchDTO> calculateCategoryMatches(StudentProfileDTO studentProfile, List<JobProfileDTO> jobs) {
        return jobs.stream()
                .map(job -> new CategoryMatchDTO(
                        job.title(),
                        calculateMatch(studentProfile, job).matchScore(),
                        1
                ))
                .sorted(Comparator.comparingDouble(CategoryMatchDTO::matchScore).reversed())
                .toList();
    }

    private DimensionScoreDTO calculateDimensionScores(StudentProfileDTO studentProfile, JobProfileDTO jobProfile) {
        return new DimensionScoreDTO(
                round(calculateBasicRequirements(studentProfile, jobProfile)),
                round(calculateProfessionalSkills(studentProfile, jobProfile)),
                round(calculateProfessionalQuality(studentProfile)),
                round(calculateGrowthPotential(studentProfile))
        );
    }

    private double calculateBasicRequirements(StudentProfileDTO studentProfile, JobProfileDTO jobProfile) {
        double targetPositionScore = calculateTargetPositionScore(studentProfile.jobPreference(), jobProfile);
        double cityScore = calculateCityScore(studentProfile.jobPreference(), jobProfile.city());
        double salaryScore = calculateSalaryScore(studentProfile.jobPreference(), jobProfile.salaryRange());
        double educationScore = calculateEducationScore(studentProfile.basicInfo() == null ? null : studentProfile.basicInfo().education());
        double majorScore = calculateMajorScore(studentProfile.basicInfo() == null ? null : studentProfile.basicInfo().major(), jobProfile);
        return clamp(targetPositionScore + cityScore + salaryScore + educationScore + majorScore);
    }

    private double calculateTargetPositionScore(JobPreferenceDTO preference, JobProfileDTO jobProfile) {
        if (preference == null || blank(preference.expectedPosition())) {
            return 22;
        }
        String expected = normalize(preference.expectedPosition());
        String title = normalize(jobProfile.title());
        String category = normalize(resolveWeightCategory(jobProfile));
        if (expected.equals(title) || expected.equals(category)) return 38;
        if (title.contains(expected) || expected.contains(title)) return 34;
        if (category.contains(expected) || expected.contains(category)) return 30;
        return 10;
    }

    private double calculateCityScore(JobPreferenceDTO preference, String city) {
        if (preference == null || blank(preference.expectedCity()) || blank(city)) {
            return 14;
        }
        String expected = normalize(preference.expectedCity());
        String target = normalize(city);
        if (expected.equals(target)) return 24;
        if (target.contains(expected) || expected.contains(target)) return 21;
        if (expected.length() >= 2 && target.length() >= 2 && expected.substring(0, 2).equals(target.substring(0, 2))) return 18;
        return 8;
    }

    private double calculateSalaryScore(JobPreferenceDTO preference, String salaryRange) {
        if (preference == null || blank(preference.expectedSalary()) || blank(salaryRange)) {
            return 12;
        }
        int expected = parseSalaryMidpoint(preference.expectedSalary());
        int target = parseSalaryMidpoint(salaryRange);
        if (expected <= 0 || target <= 0) {
            return 12;
        }
        double gap = Math.abs(expected - target) / (double) Math.max(expected, target);
        if (gap <= 0.10) return 20;
        if (gap <= 0.20) return 17;
        if (gap <= 0.35) return 13;
        return 6;
    }

    private double calculateEducationScore(String education) {
        if (blank(education)) {
            return 5;
        }
        String normalized = education.toLowerCase(Locale.ROOT);
        if (normalized.contains("博士")) return 10;
        if (normalized.contains("硕士") || normalized.contains("研究生")) return 9;
        if (normalized.contains("本科") || normalized.contains("学士")) return 8;
        if (normalized.contains("大专") || normalized.contains("专科")) return 6;
        return 5;
    }

    private double calculateMajorScore(String major, JobProfileDTO jobProfile) {
        if (blank(major)) {
            return 4;
        }
        String normalizedMajor = normalize(major);
        if (containsAny(normalizedMajor, "计算机", "软件", "信息", "电子", "通信", "自动化", "人工智能", "数据")) {
            return 8;
        }
        if (containsAny(normalizedMajor, normalize(jobProfile.title()), normalize(resolveWeightCategory(jobProfile)))) {
            return 6;
        }
        return 4;
    }

    private double calculateProfessionalSkills(StudentProfileDTO studentProfile, JobProfileDTO jobProfile) {
        List<String> skills = safeList(studentProfile.skills());
        List<String> requiredSkills = careerAiDecisionService.refineConcreteSkills(jobProfile);
        if (requiredSkills == null) {
            requiredSkills = List.of();
        }
        if (requiredSkills.isEmpty()) {
            requiredSkills = safeList(jobProfile.requiredSkills());
        }
        List<String> certificates = safeList(studentProfile.certificates());
        List<String> requiredCertificates = safeList(jobProfile.requiredCertificates());

        Set<String> normalizedStudentSkills = skills.stream().map(this::normalize).collect(Collectors.toSet());
        List<String> normalizedRequiredSkills = requiredSkills.stream().map(this::normalize).filter(value -> !value.isBlank()).toList();
        List<String> normalizedRequiredCertificates = requiredCertificates.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank() && !"无".equals(value))
                .toList();
        List<String> normalizedStudentCertificates = certificates.stream().map(this::normalize).toList();

        long matchedRequiredSkillCount = normalizedRequiredSkills.stream()
                .filter(required -> normalizedStudentSkills.stream().anyMatch(student -> student.contains(required) || required.contains(student)))
                .count();
        double coreSkillScore = normalizedRequiredSkills.isEmpty() ? 42 : (matchedRequiredSkillCount * 65.0 / normalizedRequiredSkills.size());

        long matchedCertificateCount = normalizedRequiredCertificates.stream()
                .filter(required -> normalizedStudentCertificates.stream().anyMatch(cert -> cert.contains(required) || required.contains(cert)))
                .count();
        double certificateScore = normalizedRequiredCertificates.isEmpty()
                ? 12
                : Math.min(20, matchedCertificateCount * 20.0 / normalizedRequiredCertificates.size());

        double toolBreadthScore = Math.min(8, skills.size() * 1.2);
        double extraSkillScore = Math.min(15, Math.max(0, skills.size() - matchedRequiredSkillCount) * 1.8);
        return clamp(coreSkillScore + certificateScore + toolBreadthScore + extraSkillScore);
    }

    private double calculateProfessionalQuality(StudentProfileDTO studentProfile) {
        SoftAbilitiesDTO abilities = studentProfile.softAbilities();
        if (abilities == null) {
            return 55;
        }
        double communication = scale(abilities.communication(), 30);
        double stressTolerance = scale(abilities.stressTolerance(), 25);
        double internship = scale(abilities.internship(), 20);
        double execution = scale(abilities.professionalSkills(), 15);
        double innovation = scale(abilities.innovation(), 10);
        return clamp(communication + stressTolerance + internship + execution + innovation);
    }

    private double calculateGrowthPotential(StudentProfileDTO studentProfile) {
        SoftAbilitiesDTO abilities = studentProfile.softAbilities();
        if (abilities == null) {
            return 55;
        }
        double learning = scale(abilities.learning(), 35);
        double innovation = scale(abilities.innovation(), 20);
        double internship = scale(abilities.internship(), 15);
        double skillBreadth = Math.min(20, safeList(studentProfile.skills()).size() * 2.5);
        double certificatePotential = Math.min(10, safeList(studentProfile.certificates()).size() * 3.0);
        return clamp(learning + innovation + internship + skillBreadth + certificatePotential);
    }

    private double calculateTargetPreferenceBonus(StudentProfileDTO studentProfile, JobProfileDTO jobProfile) {
        JobPreferenceDTO preference = studentProfile == null ? null : studentProfile.jobPreference();
        String expected = preference == null ? "" : normalize(preference.expectedPosition());
        if (blank(expected)) {
            return 0.0;
        }

        String title = normalize(jobProfile.title());
        String category = normalize(resolveWeightCategory(jobProfile));
        String desiredCategory = normalize(inferDesiredCategory(studentProfile));

        if (expected.equals(title) || title.contains(expected) || expected.contains(title)) {
            return 14.0;
        }
        if (category.contains(expected) || expected.contains(category)) {
            return 11.0;
        }
        if (!desiredCategory.isBlank() && desiredCategory.equals(category)) {
            return 8.0;
        }
        return 0.0;
    }

    private List<String> buildRecommendations(StudentProfileDTO studentProfile, JobProfileDTO jobProfile, DimensionScoreDTO scores) {
        List<String> recommendations = new ArrayList<>();
        List<String> skills = safeList(studentProfile.skills()).stream().map(this::normalize).toList();

        List<String> missingSkills = safeList(jobProfile.requiredSkills()).stream()
                .filter(requiredSkill -> skills.stream().noneMatch(skill -> skill.contains(normalize(requiredSkill)) || normalize(requiredSkill).contains(skill)))
                .distinct()
                .limit(4)
                .toList();

        if (!missingSkills.isEmpty()) {
            recommendations.add("优先补齐岗位核心技能：" + String.join("、", missingSkills) + "。");
        }
        if (scores.basicRequirements() < 70) {
            recommendations.add("目标岗位、目标城市或薪资预期与当前岗位样本差异较大，建议先校准求职方向。");
        }
        if (scores.professionalSkills() < 70) {
            recommendations.add("硬技能或证书匹配不足，建议围绕岗位要求补足项目成果与可验证证据。");
        }
        if (scores.professionalQuality() < 70) {
            recommendations.add("需要补充能体现沟通协作、执行推进和抗压能力的实践经历。");
        }
        if (scores.growthPotential() < 70) {
            recommendations.add("建议增加持续学习计划和阶段性交付物，提升中长期成长确定性。");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("当前画像与岗位较为匹配，建议继续强化项目成果表达与面试案例准备。");
        }
        return recommendations;
    }

    private double scale(int value, double maxScore) {
        int safeValue = Math.max(0, Math.min(100, value));
        return maxScore * safeValue / 100.0;
    }

    private int parseSalaryMidpoint(String salary) {
        if (blank(salary)) {
            return 0;
        }
        String normalized = salary.toLowerCase(Locale.ROOT)
                .replace("k", "000")
                .replace("千", "000")
                .replace("万", "0000");
        List<Integer> numbers = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(normalized);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        if (numbers.isEmpty()) {
            return 0;
        }
        if (numbers.size() == 1) {
            return numbers.get(0);
        }
        return (numbers.get(0) + numbers.get(1)) / 2;
    }

    private String resolveWeightCategory(JobProfileDTO jobProfile) {
        String title = normalize(jobProfile.title());
        String category = normalize(normalizeCategory(jobProfile.category()));
        String skills = safeList(jobProfile.requiredSkills()).stream()
                .map(this::normalize)
                .reduce((left, right) -> left + " " + right)
                .orElse("");

        if (containsAny(title, "实施", "交付") || containsAny(category, "实施", "交付")
                || containsAny(skills, "arcgis", "gis", "erp", "mes", "wms", "plm", "oracle")) {
            return CATEGORY_IMPLEMENTATION;
        }
        if (containsAny(title, "技术支持", "售后", "客户支持") || containsAny(category, "技术支持", "售后", "客户支持")
                || containsAny(skills, "support", "helpdesk")) {
            return CATEGORY_SUPPORT;
        }
        if (containsAny(title, "硬件测试", "板卡测试", "芯片测试") || containsAny(category, "硬件测试")
                || containsAny(skills, "mcu", "can", "lin", "autosar")) {
            return CATEGORY_HARDWARE_TEST;
        }
        if (containsAny(title, "测试", "qa") || containsAny(category, "测试", "qa")
                || containsAny(skills, "selenium", "jmeter", "postman")) {
            return CATEGORY_SOFTWARE_TEST;
        }
        if (containsAny(title, "运维", "devops", "sre") || containsAny(category, "运维", "devops", "sre")
                || containsAny(skills, "docker", "kubernetes", "jenkins")) {
            return CATEGORY_DEVOPS;
        }
        if (containsAny(title, "前端") || containsAny(category, "前端")
                || containsAny(skills, "vue", "react", "javascript", "typescript", "html", "css")) {
            return CATEGORY_FRONTEND;
        }
        if (containsAny(title, "python") || containsAny(category, "python")
                || containsAny(skills, "python", "django", "flask", "fastapi")) {
            return CATEGORY_PYTHON;
        }
        if (containsAny(title, "c++", "嵌入式", "单片机") || containsAny(category, "c++", "嵌入式")
                || containsAny(skills, "c++", "qt", "嵌入式", "单片机")) {
            return CATEGORY_GENERAL;
        }
        if (containsAny(title, "算法", "机器学习", "深度学习", "nlp", "cv") || containsAny(category, "算法", "机器学习")
                || containsAny(skills, "机器学习", "深度学习", "pytorch", "tensorflow")) {
            return CATEGORY_ALGORITHM;
        }
        if (containsAny(title, "数据分析", "商业分析", "bi") || containsAny(category, "数据分析", "bi")
                || containsAny(skills, "tableau", "powerbi", "sql")) {
            return CATEGORY_DATA;
        }
        if (containsAny(title, "科研", "研究") || containsAny(category, "科研", "研究")) {
            return CATEGORY_RESEARCH;
        }
        if (containsAny(title, "产品经理", "项目经理", "pm") || containsAny(category, "产品经理", "项目经理", "pm")) {
            return CATEGORY_PM;
        }
        if (containsAny(title, "运营", "推广", "增长") || containsAny(category, "运营", "推广", "增长")) {
            return CATEGORY_OPERATIONS;
        }
        if (containsAny(title, "java") || containsAny(category, "java")
                || containsAny(skills, "java", "spring", "mybatis", "redis")) {
            return CATEGORY_JAVA;
        }

        String normalizedCategory = normalizeCategory(jobProfile.category());
        return CATEGORY_WEIGHTS.containsKey(normalizedCategory) ? normalizedCategory : CATEGORY_GENERAL;
    }

    private String inferDesiredCategory(StudentProfileDTO studentProfile) {
        if (studentProfile == null) {
            return CATEGORY_GENERAL;
        }

        List<String> candidates = List.of(
                CATEGORY_JAVA, CATEGORY_FRONTEND, CATEGORY_PYTHON, CATEGORY_ALGORITHM, CATEGORY_DATA,
                CATEGORY_SOFTWARE_TEST, CATEGORY_HARDWARE_TEST, CATEGORY_IMPLEMENTATION, CATEGORY_SUPPORT,
                CATEGORY_DEVOPS, CATEGORY_PM, CATEGORY_OPERATIONS, CATEGORY_RESEARCH, CATEGORY_GENERAL
        );
        String aiCategory = careerAiDecisionService.resolveProfileIntent(studentProfile, candidates);
        if (!blank(aiCategory)) {
            return aiCategory;
        }

        String expected = studentProfile.jobPreference() == null ? "" : normalize(studentProfile.jobPreference().expectedPosition());
        String skills = safeList(studentProfile.skills()).stream()
                .map(this::normalize)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
        String source = expected + " " + skills;

        if (containsAny(source, "java", "spring", "mybatis", "redis")) return CATEGORY_JAVA;
        if (containsAny(source, "前端", "frontend", "javascript", "typescript", "vue", "react")) return CATEGORY_FRONTEND;
        if (containsAny(source, "测试", "qa", "postman", "jmeter", "selenium")) return CATEGORY_SOFTWARE_TEST;
        if (containsAny(source, "硬件", "示波器", "pcba", "can", "lin")) return CATEGORY_HARDWARE_TEST;
        if (containsAny(source, "实施", "erp", "mes", "plm", "gis")) return CATEGORY_IMPLEMENTATION;
        if (containsAny(source, "技术支持", "support", "售后")) return CATEGORY_SUPPORT;
        if (containsAny(source, "运维", "devops", "docker", "kubernetes")) return CATEGORY_DEVOPS;
        if (containsAny(source, "项目经理", "产品经理", "pm", "产品")) return CATEGORY_PM;
        if (containsAny(source, "推广", "运营", "增长")) return CATEGORY_OPERATIONS;
        if (containsAny(source, "科研", "研究", "论文")) return CATEGORY_RESEARCH;
        if (containsAny(source, "数据分析", "sql", "powerbi", "tableau")) return CATEGORY_DATA;
        if (containsAny(source, "python", "django", "flask", "fastapi")) return CATEGORY_PYTHON;
        return CATEGORY_GENERAL;
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && (value.contains(candidate) || candidate.contains(value))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("/", "").replace("+", "").replace(" ", "");
    }

    private String normalizeCategory(String value) {
        if (blank(value)) {
            return CATEGORY_GENERAL;
        }
        return switch (value) {
            case "frontend" -> CATEGORY_FRONTEND;
            case "backend" -> CATEGORY_JAVA;
            case "testing" -> CATEGORY_SOFTWARE_TEST;
            case "data" -> CATEGORY_DATA;
            case "other" -> CATEGORY_GENERAL;
            default -> value;
        };
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static Map<String, Double> weights(double basic, double skills, double quality, double growth) {
        return Map.of("basic", basic, "skills", skills, "quality", quality, "growth", growth);
    }
}
