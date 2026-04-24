package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.ai.llm.AIService;
import com.zust.qyf.careeragent.domain.dto.ResumeDTO;
import com.zust.qyf.careeragent.domain.dto.student.AbilityDescriptionsDTO;
import com.zust.qyf.careeragent.domain.dto.student.BasicInfoDTO;
import com.zust.qyf.careeragent.domain.dto.student.InternshipExperienceDTO;
import com.zust.qyf.careeragent.domain.dto.student.JobPreferenceDTO;
import com.zust.qyf.careeragent.domain.dto.student.ProjectExperienceDTO;
import com.zust.qyf.careeragent.domain.dto.student.SoftAbilitiesDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileEvaluationDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zust.qyf.careeragent.utils.ResumeUtil.cleanFile;
import static com.zust.qyf.careeragent.utils.ResumeUtil.validateFile;

@Service
public class ResumeProfileApplicationService {
    private static final Pattern CITY_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,8})(?:市)?");
    private static final Pattern SALARY_PATTERN = Pattern.compile("(?:期望薪资|薪资期望|薪酬要求)[:：\\s]*([0-9]{1,2}\\s*[-~至]\\s*[0-9]{1,2}\\s*[kK万])");

    private static final Pattern EXPLICIT_NAME_PATTERN = Pattern.compile("(?:姓名|名字|Name)[:：\\s]*([\\u4e00-\\u9fa5A-Za-z·]{2,30})");
    private static final Pattern EXPLICIT_SCHOOL_PATTERN = Pattern.compile("(?:学校|院校|毕业院校|教育背景)[:：\\s]*([\\u4e00-\\u9fa5]{2,30}(?:大学|学院))");
    private static final Pattern EXPLICIT_MAJOR_PATTERN = Pattern.compile("(?:专业|主修专业)[:：\\s]*([\\u4e00-\\u9fa5A-Za-z0-9（）()]{2,40})");

    private final AIService aiService;
    private final StudentProfilePersistenceService studentProfilePersistenceService;

    public ResumeProfileApplicationService(AIService aiService,
                                           StudentProfilePersistenceService studentProfilePersistenceService) {
        this.aiService = aiService;
        this.studentProfilePersistenceService = studentProfilePersistenceService;
    }

    public ResumeDTO analyseResume(MultipartFile file) {
        validateFile(file);
        String cleanText = cleanFile(file);
        return parseResumeText(cleanText);
    }

    public StudentProfileDTO createStudentProfile(MultipartFile file,
                                                  String expectedPosition,
                                                  String expectedSalary,
                                                  String expectedCity) {
        validateFile(file);
        String cleanText = cleanFile(file);
        ResumeDTO resumeDTO = parseResumeText(cleanText);
        return mapToStudentProfile(resumeDTO, cleanText, expectedPosition, expectedSalary, expectedCity);
    }

    public StudentProfileDTO createManualStudentProfile(StudentProfileDTO profile) {
        if (profile == null || profile.basicInfo() == null) {
            throw new IllegalArgumentException("manual profile must not be empty");
        }
        String studentId = blank(profile.studentId()) ? UUID.randomUUID().toString() : profile.studentId();
        List<String> skills = profile.skills() == null ? List.of() : profile.skills().stream().filter(skill -> !blank(skill)).distinct().toList();
        List<String> certificates = profile.certificates() == null ? List.of() : profile.certificates().stream().filter(cert -> !blank(cert)).distinct().toList();
        List<InternshipExperienceDTO> internshipExperiences = normalizeInternshipExperiences(profile.internshipExperiences());
        List<ProjectExperienceDTO> projectExperiences = normalizeProjectExperiences(profile.projectExperiences());
        SoftAbilitiesDTO softAbilities = profile.softAbilities() == null
                ? new SoftAbilitiesDTO(60, 60, 60, 60, 60, 60, 60)
                : profile.softAbilities();
        JobPreferenceDTO preference = profile.jobPreference() == null
                ? new JobPreferenceDTO("", "", "")
                : profile.jobPreference();

        return new StudentProfileDTO(
                studentId,
                profile.basicInfo(),
                skills,
                certificates,
                softAbilities,
                new JobPreferenceDTO(
                        fallback(preference.expectedPosition(), inferExpectedPositionFromSkills(skills)),
                        fallback(preference.expectedSalary(), ""),
                        fallback(preference.expectedCity(), "")
                ),
                profile.abilityDescriptions(),
                internshipExperiences,
                projectExperiences
        );
    }

    public Optional<StudentProfileDTO> getStudentProfile(String studentId) {
        return studentProfilePersistenceService.findByStudentId(studentId);
    }

    public Optional<StudentProfileDTO> getCurrentUserProfile(Long userId) {
        return studentProfilePersistenceService.findByUserId(userId);
    }

    public StudentProfileDTO saveForUser(Long userId, StudentProfileDTO profile) {
        return studentProfilePersistenceService.saveForUser(userId, profile);
    }

    public StudentProfileEvaluationDTO evaluateStudentProfile(StudentProfileDTO profile) {
        if (profile == null) {
            throw new IllegalArgumentException("studentProfile must not be null");
        }

        int totalFields = 13;
        int filledFields = 0;
        BasicInfoDTO basic = profile.basicInfo();
        if (basic != null && !blank(basic.name())) filledFields++;
        if (basic != null && !blank(basic.education())) filledFields++;
        if (basic != null && !blank(basic.major())) filledFields++;
        if (basic != null && !blank(basic.school())) filledFields++;
        if (profile.skills() != null && !profile.skills().isEmpty()) filledFields++;
        if (profile.certificates() != null && !profile.certificates().isEmpty()) filledFields++;
        if (profile.jobPreference() != null && !blank(profile.jobPreference().expectedPosition())) filledFields++;
        if (profile.jobPreference() != null && !blank(profile.jobPreference().expectedSalary())) filledFields++;
        if (profile.jobPreference() != null && !blank(profile.jobPreference().expectedCity())) filledFields++;
        if (profile.softAbilities() != null) filledFields++;
        if (hasAbilityDescriptions(profile.abilityDescriptions())) filledFields++;
        if (profile.internshipExperiences() != null && !profile.internshipExperiences().isEmpty()) filledFields++;
        if (profile.projectExperiences() != null && !profile.projectExperiences().isEmpty()) filledFields++;
        double completenessScore = round(filledFields * 100.0 / totalFields);

        SoftAbilitiesDTO abilities = profile.softAbilities() == null
                ? new SoftAbilitiesDTO(0, 0, 0, 0, 0, 0, 0)
                : profile.softAbilities();
        double skillScore = Math.min(100, (profile.skills() == null ? 0 : profile.skills().size() * 13.0));
        double certScore = Math.min(100, (profile.certificates() == null ? 0 : profile.certificates().size() * 18.0));
        double softScore = round((abilities.innovation() + abilities.learning() + abilities.stressTolerance() + abilities.communication()) / 4.0);
        double internshipScore = abilities.internship();
        double competitivenessScore = round(skillScore * 0.42 + certScore * 0.18 + softScore * 0.25 + internshipScore * 0.15);

        Map<String, Double> dimensions = new LinkedHashMap<>();
        dimensions.put("professional_skills", round(skillScore));
        dimensions.put("certificates", round(certScore));
        dimensions.put("soft_abilities", softScore);
        dimensions.put("internship", round(internshipScore));
        return new StudentProfileEvaluationDTO(completenessScore, competitivenessScore, dimensions);
    }

    public StudentProfileDTO mapToStudentProfile(ResumeDTO resumeDTO,
                                                 String cleanText,
                                                 String expectedPosition,
                                                 String expectedSalary,
                                                 String expectedCity) {
        ResumeDTO.BasicInformation basic = resumeDTO.basicInformation();
        ResumeDTO.EducationExperience education = resumeDTO.educationExperience() != null && !resumeDTO.educationExperience().isEmpty()
                ? resumeDTO.educationExperience().get(0)
                : null;
        ResumeDTO.ResumeScore score = resumeDTO.score();

        List<String> skills = resumeDTO.skill() == null || resumeDTO.skill().hardSkills() == null
                ? List.of()
                : resumeDTO.skill().hardSkills().stream().filter(skill -> !blank(skill)).distinct().toList();
        List<String> certificates = extractCertificates(resumeDTO);
        List<InternshipExperienceDTO> internshipExperiences = mapInternshipExperiences(resumeDTO);
        List<ProjectExperienceDTO> projectExperiences = mapProjectExperiences(resumeDTO);

        String finalExpectedPosition = resolveExpectedPosition(resumeDTO, expectedPosition, cleanText);
        String finalExpectedSalary = resolveExpectedSalary(cleanText, expectedSalary);
        String finalExpectedCity = resolveExpectedCity(resumeDTO, cleanText, expectedCity);
        String resolvedName = firstNonBlank(
                basic == null ? "" : safe(basic.name()),
                extractExplicitField(cleanText, EXPLICIT_NAME_PATTERN)
        );
        String resolvedSchool = firstNonBlank(
                education == null ? "" : safe(education.school()),
                extractExplicitField(cleanText, EXPLICIT_SCHOOL_PATTERN)
        );
        String resolvedMajor = firstNonBlank(
                education == null ? "" : safe(education.major()),
                extractExplicitField(cleanText, EXPLICIT_MAJOR_PATTERN)
        );
        AbilityDescriptionsDTO abilityDescriptions = generateAbilityDescriptions(cleanText, resumeDTO);

        return new StudentProfileDTO(
                UUID.randomUUID().toString(),
                new BasicInfoDTO(
                        resolvedName,
                        education == null ? "" : safe(education.degree()),
                        resolvedMajor,
                        resolvedSchool,
                        extractGraduationYear(education == null ? null : education.period())
                ),
                skills,
                certificates,
                new SoftAbilitiesDTO(
                        safeScore(score == null ? null : score.innovation()),
                        safeScore(score == null ? null : score.learning()),
                        safeScore(score == null ? null : score.stressManagement()),
                        safeScore(score == null ? null : score.communication()),
                        safeScore(score == null ? null : score.technical()),
                        safeScore(score == null ? null : score.certificatesScore()),
                        safeScore(score == null ? null : score.internship())
                ),
                new JobPreferenceDTO(finalExpectedPosition, finalExpectedSalary, finalExpectedCity),
                abilityDescriptions,
                internshipExperiences,
                projectExperiences
        );
    }

    private ResumeDTO parseResumeText(String cleanText) {
        try {
            return CompletableFuture.supplyAsync(() -> aiService.analyseResume(cleanText))
                    .get(120, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Kimi resume analysis failed", e);
        }
    }

    private AbilityDescriptionsDTO generateAbilityDescriptions(String cleanText, ResumeDTO resumeDTO) {
        try {
            return CompletableFuture.supplyAsync(() -> aiService.generateAbilityDescriptions(cleanText, resumeDTO))
                    .get(120, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Kimi ability description generation failed", e);
        }
    }

    private List<String> extractCertificates(ResumeDTO resumeDTO) {
        List<String> certificates = new ArrayList<>();
        if (resumeDTO.score() != null && resumeDTO.score().advantages() != null) {
            resumeDTO.score().advantages().stream()
                    .filter(item -> item != null && !item.isBlank())
                    .filter(item -> item.contains("证") || item.contains("级") || item.contains("认证") || item.contains("奖"))
                    .map(String::trim)
                    .forEach(certificates::add);
        }
        return certificates.stream().distinct().toList();
    }

    private String resolveExpectedPosition(ResumeDTO resumeDTO, String expectedPosition, String cleanText) {
        if (!blank(expectedPosition)) {
            return expectedPosition.trim();
        }
        if (resumeDTO.jobs() != null) {
            String preferred = pickPreferredJob(resumeDTO.jobs());
            if (!blank(preferred)) {
                return normalizeExpectedPosition(preferred);
            }
        }
        return inferExpectedPositionFromSkills(resumeDTO.skill() == null ? List.of() : resumeDTO.skill().hardSkills());
    }

    private String resolveExpectedCity(ResumeDTO resumeDTO, String cleanText, String expectedCity) {
        if (!blank(expectedCity)) {
            return expectedCity.trim();
        }
        if (resumeDTO.basicInformation() != null && !blank(resumeDTO.basicInformation().city())) {
            return resumeDTO.basicInformation().city();
        }
        Matcher matcher = CITY_PATTERN.matcher(cleanText == null ? "" : cleanText);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String resolveExpectedSalary(String cleanText, String expectedSalary) {
        if (!blank(expectedSalary)) {
            return expectedSalary.trim().toUpperCase();
        }
        if (cleanText == null || cleanText.isBlank()) {
            return "";
        }
        Matcher matcher = SALARY_PATTERN.matcher(cleanText);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", "").toUpperCase();
        }
        return "";
    }

    private String inferExpectedPositionFromSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }
        String joined = String.join(" ", skills).toLowerCase();
        if (containsAny(joined, "vue", "react", "javascript", "typescript", "html", "css")) return "前端开发";
        if (containsAny(joined, "java", "spring", "mybatis", "redis")) return "Java开发";
        if (containsAny(joined, "python", "django", "flask", "pandas")) return "Python开发";
        if (containsAny(joined, "selenium", "jmeter", "postman")) return "软件测试";
        if (containsAny(joined, "docker", "kubernetes", "jenkins", "linux")) return "运维/DevOps";
        if (containsAny(joined, "c++", "qt", "嵌入式")) return "C/C++开发";
        if (containsAny(joined, "机器学习", "tensorflow", "pytorch", "算法")) return "算法工程师";
        if (containsAny(joined, "sql", "tableau", "power bi", "bi")) return "数据分析";
        return "综合开发";
    }

    private String normalizeExpectedPosition(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.contains("前端")) return "前端开发";
        if (normalized.contains("Java")) return "Java开发";
        if (normalized.contains("Python")) return "Python开发";
        if (normalized.contains("产品") || normalized.contains("项目经理") || normalized.equalsIgnoreCase("pm")) return "产品/项目经理";
        if (normalized.contains("测试")) return "软件测试";
        if (normalized.contains("实施")) return "实施工程师";
        if (normalized.contains("支持")) return "技术支持工程师";
        if (normalized.contains("运维")) return "运维/DevOps";
        if (normalized.contains("数据")) return "数据分析";
        if (normalized.contains("算法")) return "算法工程师";
        if (normalized.contains("科研")) return "科研人员";
        return normalized;
    }

    private String extractGraduationYear(String period) {
        if (period == null || period.isBlank()) {
            return "";
        }
        String[] parts = period.split("-");
        return parts.length > 1 ? parts[parts.length - 1].trim() : parts[0].trim();
    }

    private int safeScore(Integer score) {
        return score == null ? 0 : Math.max(0, Math.min(100, score));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String buildProfessionalSkillDescription(ResumeDTO resumeDTO) {
        List<String> hardSkills = resumeDTO.skill() == null || resumeDTO.skill().hardSkills() == null
                ? List.of()
                : normalizeTexts(resumeDTO.skill().hardSkills());
        List<String> projectSignals = resumeDTO.projects() == null
                ? List.of()
                : resumeDTO.projects().stream()
                .flatMap(project -> normalizeTexts(project.techStacks()).stream())
                .distinct()
                .filter(skill -> !hardSkills.contains(skill))
                .limit(4)
                .toList();

        if (hardSkills.isEmpty() && projectSignals.isEmpty()) {
            return "当前简历里还没有提取到足够明确的技术栈或工具使用证据。";
        }

        List<String> evidence = new ArrayList<>();
        if (!hardSkills.isEmpty()) {
            evidence.add("已识别的核心技术包括" + String.join("、", hardSkills.stream().limit(6).toList()));
        }
        if (!projectSignals.isEmpty()) {
            evidence.add("项目中还使用过" + String.join("、", projectSignals));
        }
        return String.join("；", evidence) + "。";
    }

    private String buildCertificateDescription(List<String> certificates, ResumeDTO.ResumeScore score) {
        if (!certificates.isEmpty()) {
            return "当前识别到的证书或荣誉包括：" + String.join("、", certificates) + "。建议在简历中标明等级、时间和岗位相关性。";
        }
        int certificateScore = score == null || score.certificatesScore() == null ? 0 : score.certificatesScore();
        if (certificateScore >= 70) {
            return "证书强度尚可，但仍建议把证书与目标岗位要求做更明确的映射。";
        }
        return "证书和高信号荣誉仍偏弱，可以补充软考、英语等级、竞赛获奖或行业认证。";
    }

    private String buildSoftSkillDescription(ResumeDTO.ResumeScore score) {
        if (score == null) {
            return "从现有材料来看，你已经具备比较稳定的学习投入和执行意识。后续如果能再补充团队协作、问题推进或阶段成果方面的具体例子，整个人的软实力会更立体。";
        }
        List<String> strengths = extractSoftStrengthPhrases(score);
        if (strengths.isEmpty()) {
            return "从现有材料来看，你整体给人的感觉是踏实、稳定，也有继续成长的空间。后续建议把课堂、项目或实践中体现主动性和协作力的例子再写具体一点。";
        }
        return buildTeacherStyleSoftSkillComment(strengths, false, false);
    }

    private String buildSoftSkillDescription(ResumeDTO resumeDTO, ResumeDTO.ResumeScore score) {
        List<String> softSkills = resumeDTO.skill() == null || resumeDTO.skill().softSkills() == null
                ? List.of()
                : normalizeTexts(resumeDTO.skill().softSkills());
        List<String> strengths = new ArrayList<>(softSkills.stream()
                .map(this::normalizeSoftSkillPhrase)
                .filter(item -> !blank(item))
                .distinct()
                .limit(3)
                .toList());

        strengths.addAll(extractSoftStrengthPhrases(score).stream()
                .filter(item -> !strengths.contains(item))
                .limit(Math.max(0, 3 - strengths.size()))
                .toList());

        boolean hasWorkOrInternship = resumeDTO.workExperience() != null && !resumeDTO.workExperience().isEmpty();
        boolean hasProjects = resumeDTO.projects() != null && !resumeDTO.projects().isEmpty();

        if (!strengths.isEmpty()) {
            return buildTeacherStyleSoftSkillComment(strengths, hasWorkOrInternship, hasProjects);
        }
        return buildSoftSkillDescription(score);
    }

    private String buildScoreDescription(String label, Integer score, List<String> advantages) {
        int safe = safeScore(score);
        String level = safe >= 85 ? "较强" : safe >= 70 ? "中上" : safe >= 60 ? "基本达标" : "偏弱";
        String advantage = advantages == null ? "" : advantages.stream().filter(item -> !blank(item)).findFirst().orElse("");
        if (blank(advantage)) {
            return label + "当前判断为" + level + "（" + safe + "分），建议补充能直接证明该能力的经历或结果。";
        }
        return label + "当前判断为" + level + "（" + safe + "分），可以继续围绕“" + advantage + "”补充可验证证据。";
    }

    private String buildInternshipDescription(ResumeDTO resumeDTO, ResumeDTO.ResumeScore score) {
        if (resumeDTO.workExperience() != null && !resumeDTO.workExperience().isEmpty()) {
            ResumeDTO.WorkExperience experience = resumeDTO.workExperience().get(0);
            return "最近一段实践经历是 " + safe(experience.company()) + " 的 " + safe(experience.position())
                    + "，建议继续补充量化结果和岗位相关性。";
        }
        int internshipScore = score == null || score.internship() == null ? 0 : score.internship();
        if (internshipScore >= 70) {
            return "实践背景基础还可以，但需要把职责范围、协作方式和结果写得更具体。";
        }
        return "实践经历仍偏弱，建议尽快补充实习、项目或校企合作经历。";
    }

    private List<InternshipExperienceDTO> mapInternshipExperiences(ResumeDTO resumeDTO) {
        if (resumeDTO.workExperience() == null) {
            return List.of();
        }
        return normalizeInternshipExperiences(resumeDTO.workExperience().stream()
                .map(work -> new InternshipExperienceDTO(
                        safe(work.company()),
                        safe(work.position()),
                        safe(work.period()),
                        safe(work.achievement())
                ))
                .toList());
    }

    private List<ProjectExperienceDTO> mapProjectExperiences(ResumeDTO resumeDTO) {
        if (resumeDTO.projects() == null) {
            return List.of();
        }
        return normalizeProjectExperiences(resumeDTO.projects().stream()
                .map(project -> new ProjectExperienceDTO(
                        safe(project.name()),
                        safe(project.role()),
                        safe(project.description()),
                        normalizeTexts(project.techStacks()),
                        safe(project.highlight())
                ))
                .toList());
    }

    private List<InternshipExperienceDTO> normalizeInternshipExperiences(List<InternshipExperienceDTO> experiences) {
        if (experiences == null) {
            return List.of();
        }
        return experiences.stream()
                .filter(experience -> experience != null)
                .map(experience -> new InternshipExperienceDTO(
                        safe(experience.company()).trim(),
                        safe(experience.position()).trim(),
                        safe(experience.period()).trim(),
                        safe(experience.achievement()).trim()
                ))
                .filter(experience -> !blank(experience.company())
                        || !blank(experience.position())
                        || !blank(experience.period())
                        || !blank(experience.achievement()))
                .toList();
    }

    private List<ProjectExperienceDTO> normalizeProjectExperiences(List<ProjectExperienceDTO> projects) {
        if (projects == null) {
            return List.of();
        }
        return projects.stream()
                .filter(project -> project != null)
                .map(project -> new ProjectExperienceDTO(
                        safe(project.name()).trim(),
                        safe(project.role()).trim(),
                        safe(project.description()).trim(),
                        normalizeTexts(project.techStacks()),
                        safe(project.highlight()).trim()
                ))
                .filter(project -> !blank(project.name())
                        || !blank(project.role())
                        || !blank(project.description())
                        || (project.techStacks() != null && !project.techStacks().isEmpty())
                        || !blank(project.highlight()))
                .toList();
    }

    private List<String> normalizeTexts(List<String> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(item -> !blank(item))
                .map(String::trim)
                .distinct()
                .toList();
    }

    private boolean hasAbilityDescriptions(AbilityDescriptionsDTO descriptions) {
        return descriptions != null
                && (!blank(descriptions.professionalSkill())
                || !blank(descriptions.softSkill())
                || !blank(descriptions.certificate())
                || !blank(descriptions.innovation())
                || !blank(descriptions.learning())
                || !blank(descriptions.stressTolerance())
                || !blank(descriptions.communication())
                || !blank(descriptions.internship()));
    }

    private boolean hasMeaningfulScore(Integer score) {
        return score != null && score > 0;
    }

    private List<String> extractSoftStrengthPhrases(ResumeDTO.ResumeScore score) {
        if (score == null) {
            return List.of();
        }
        List<String> strengths = new ArrayList<>();
        if (hasPositiveSoftScore(score.learning())) {
            strengths.add("学习状态比较主动，上手新内容通常较快");
        }
        if (hasPositiveSoftScore(score.communication())) {
            strengths.add("沟通和配合意识较好，适合放在协作型任务中继续打磨");
        }
        if (hasPositiveSoftScore(score.stressManagement())) {
            strengths.add("做事相对稳，面对任务压力时能够保持基本节奏");
        }
        if (hasPositiveSoftScore(score.innovation())) {
            strengths.add("愿意主动思考和优化，具备一定的进取心");
        }
        return strengths;
    }

    private boolean hasPositiveSoftScore(Integer score) {
        return score != null && score >= 60;
    }

    private String normalizeSoftSkillPhrase(String softSkill) {
        String normalized = safe(softSkill).trim();
        if (blank(normalized)) {
            return "";
        }
        if (containsAny(normalized, "沟通", "表达", "汇报")) {
            return "沟通表达比较自然，具备较好的协作基础";
        }
        if (containsAny(normalized, "协作", "合作", "团队")) {
            return "团队协作意识不错，愿意配合任务推进";
        }
        if (containsAny(normalized, "学习", "自学", "快速学习")) {
            return "学习主动性比较好，愿意持续吸收新内容";
        }
        if (containsAny(normalized, "抗压", "稳定", "多任务")) {
            return "做事节奏比较稳，面对任务压力时能保持投入";
        }
        if (containsAny(normalized, "组织", "推进", "执行", "负责")) {
            return "执行和推进意识较强，适合在项目中继续承担责任";
        }
        if (containsAny(normalized, "创新", "优化", "主动")) {
            return "愿意思考改进方式，具备一定主动性和进取心";
        }
        return normalized + "方面有不错的基础";
    }

    private String buildTeacherStyleSoftSkillComment(List<String> strengths,
                                                     boolean hasWorkOrInternship,
                                                     boolean hasProjects) {
        List<String> picked = strengths.stream()
                .filter(item -> !blank(item))
                .distinct()
                .limit(3)
                .toList();

        if (picked.isEmpty()) {
            return "从现有材料来看，你整体给人的感觉是踏实、稳定，也有继续成长的空间。后续建议把课堂、项目或实践中体现主动性和协作力的例子再写具体一点。";
        }

        StringBuilder builder = new StringBuilder("从现有简历内容来看，你给人的整体印象是比较踏实、积极的。");
        builder.append("目前能看出的优势主要体现在：")
                .append(String.join("；", picked))
                .append("。");

        if (hasWorkOrInternship) {
            builder.append("再加上你已经有实习或实践场景，这些能力是有机会继续沉淀成稳定优势的。");
        } else if (hasProjects) {
            builder.append("如果后续能把项目中的分工、协作过程和结果再写得更具体，你的软实力会更有说服力。");
        } else {
            builder.append("后续只要继续把项目、课程或实践中的具体例子补充清楚，这部分优势会更容易被看到。");
        }
        return builder.toString();
    }

    private String pickPreferredJob(List<String> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return "";
        }
        List<String> normalizedJobs = jobs.stream()
                .filter(job -> !blank(job))
                .map(String::trim)
                .toList();
        if (normalizedJobs.isEmpty()) {
            return "";
        }
        for (String job : normalizedJobs) {
            if (!"综合开发".equals(job)) {
                return job;
            }
        }
        return normalizedJobs.get(0);
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String fallback(String value, String fallback) {
        return blank(value) ? fallback : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String extractExplicitField(String cleanText, Pattern pattern) {
        if (cleanText == null || cleanText.isBlank()) {
            return "";
        }
        Matcher matcher = pattern.matcher(cleanText);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1) == null ? "" : matcher.group(1).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!blank(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
