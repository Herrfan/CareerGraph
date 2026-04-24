package com.zust.qyf.careeragent.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.report.ProfileScoringDTO;
import com.zust.qyf.careeragent.domain.dto.student.SoftAbilitiesDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import com.zust.qyf.careeragent.infrastructure.knowledge.JobCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProfileScoringService {
    private static final Path DEFAULT_PROCESSED_PORTRAIT_DIR = Path.of(System.getProperty("user.dir"), "data", "processed", "岗位画像json");
    private static final String DEFAULT_CLASSPATH_PATTERN = "classpath:knowledge/岗位画像/*_结构化画像.json";
    
    private static final Set<String> SKILL_CATEGORIES = Set.of(
            "专业技能", "证书要求", "创新能力", "学习能力", "抗压能力", "沟通能力", "实习能力"
    );
    
    private static final Map<String, Double> CATEGORY_WEIGHTS = Map.of(
            "专业技能", 0.35,
            "证书要求", 0.10,
            "创新能力", 0.12,
            "学习能力", 0.10,
            "抗压能力", 0.12,
            "沟通能力", 0.12,
            "实习能力", 0.09
    );
    
    private final JobCatalogService jobCatalogService;
    private final ObjectMapper objectMapper;
    private volatile Map<String, JsonNode> cachedJobPortraits;
    
    @Autowired
    public ProfileScoringService(JobCatalogService jobCatalogService, ObjectMapper objectMapper) {
        this.jobCatalogService = jobCatalogService;
        this.objectMapper = objectMapper;
    }
    
    public ProfileScoringDTO scoreProfile(StudentProfileDTO studentProfile, String targetJobTitle) {
        System.out.println("[ProfileScoringService] Starting scoreProfile...");
        System.out.println("[ProfileScoringService] Target job title: " + targetJobTitle);
        System.out.println("[ProfileScoringService] Student profile: " + (studentProfile != null ? "not null" : "null"));
        
        if (studentProfile == null) {
            System.out.println("[ProfileScoringService] Student profile is null");
            return ProfileScoringDTO.create(0, 0, List.of("学生画像为空"), List.of("学生画像为空"), targetJobTitle);
        }
        
        String jobTitle = targetJobTitle != null ? targetJobTitle : 
                (studentProfile.jobPreference() != null ? studentProfile.jobPreference().expectedPosition() : null);
        
        if (jobTitle == null || jobTitle.isBlank()) {
            jobTitle = "Java";
        }
        
        System.out.println("[ProfileScoringService] Using job title: " + jobTitle);
        
        JsonNode jobPortrait = loadJobPortrait(jobTitle);
        
        if (jobPortrait == null) {
            System.out.println("[ProfileScoringService] Job portrait not found");
            return ProfileScoringDTO.create(0, 0, List.of("找不到岗位画像"), List.of("找不到岗位画像"), jobTitle);
        }
        
        System.out.println("[ProfileScoringService] Job portrait loaded successfully");
        
        List<String> completenessBreakdown = new ArrayList<>();
        List<String> competitivenessBreakdown = new ArrayList<>();
        
        double completenessScore = calculateCompletenessScore(studentProfile, jobPortrait, completenessBreakdown);
        double competitivenessScore = calculateCompetitivenessScore(studentProfile, jobPortrait, competitivenessBreakdown);
        
        System.out.println("[ProfileScoringService] Completeness score: " + completenessScore);
        System.out.println("[ProfileScoringService] Competitiveness score: " + competitivenessScore);
        
        return ProfileScoringDTO.create(completenessScore, competitivenessScore, completenessBreakdown, competitivenessBreakdown, jobTitle);
    }
    
    private double calculateCompletenessScore(StudentProfileDTO studentProfile, JsonNode jobPortrait, List<String> breakdown) {
        double score = 0;
        int totalDimensions = 6;
        
        score += checkBasicInfo(studentProfile, breakdown);
        score += checkSkills(studentProfile, jobPortrait, breakdown);
        score += checkCertificates(studentProfile, jobPortrait, breakdown);
        score += checkSoftAbilities(studentProfile, breakdown);
        score += checkExperiences(studentProfile, breakdown);
        score += checkJobPreference(studentProfile, breakdown);
        
        return Math.min(100, Math.max(0, (score / totalDimensions) * 100));
    }
    
    private double calculateCompetitivenessScore(StudentProfileDTO studentProfile, JsonNode jobPortrait, List<String> breakdown) {
        double score = 0;
        
        score += scoreSkillMatch(studentProfile, jobPortrait, breakdown);
        score += scoreSkillDepth(studentProfile, jobPortrait, breakdown);
        score += scoreCertificateRelevance(studentProfile, jobPortrait, breakdown);
        score += scoreSoftAbilities(studentProfile, breakdown);
        score += scoreExperienceQuality(studentProfile, breakdown);
        score += scoreSkillBreadth(studentProfile, breakdown);
        
        return Math.min(100, Math.max(0, score));
    }
    
    private double checkBasicInfo(StudentProfileDTO studentProfile, List<String> breakdown) {
        int filled = 0;
        int total = 4;
        if (studentProfile.basicInfo() != null) {
            if (notBlank(studentProfile.basicInfo().name())) filled++;
            if (notBlank(studentProfile.basicInfo().education())) filled++;
            if (notBlank(studentProfile.basicInfo().major())) filled++;
            if (notBlank(studentProfile.basicInfo().school())) filled++;
        }
        String status = filled >= 3 ? "✓" : "○";
        breakdown.add(status + " 基本信息：" + filled + "/" + total + " 项已填写");
        return (double) filled / total;
    }
    
    private double checkSkills(StudentProfileDTO studentProfile, JsonNode jobPortrait, List<String> breakdown) {
        List<String> studentSkills = safeList(studentProfile.skills());
        Set<String> normalizedStudentSkills = studentSkills.stream()
                .map(this::normalize)
                .collect(Collectors.toSet());
        
        List<String> jobRequiredSkills = extractJobSkills(jobPortrait);
        long matched = jobRequiredSkills.stream()
                .filter(skill -> normalizedStudentSkills.stream().anyMatch(s -> s.contains(normalize(skill)) || normalize(skill).contains(s)))
                .count();
        
        double percentage = jobRequiredSkills.isEmpty() ? 1.0 : (double) matched / jobRequiredSkills.size();
        String status = percentage >= 0.6 ? "✓" : "○";
        breakdown.add(status + " 技能匹配：" + matched + "/" + jobRequiredSkills.size() + " 项核心技能已掌握");
        return percentage;
    }
    
    private double checkCertificates(StudentProfileDTO studentProfile, JsonNode jobPortrait, List<String> breakdown) {
        List<String> studentCerts = safeList(studentProfile.certificates());
        if (studentCerts.isEmpty()) {
            breakdown.add("○ 证书：无证书信息");
            return 0;
        }
        
        List<String> jobCerts = extractJobCertificates(jobPortrait);
        long matched = studentCerts.stream()
                .filter(cert -> jobCerts.stream().anyMatch(jc -> normalize(cert).contains(normalize(jc)) || normalize(jc).contains(normalize(cert))))
                .count();
        
        String status = matched >= 1 || studentCerts.size() >= 1 ? "✓" : "○";
        breakdown.add(status + " 证书：拥有 " + studentCerts.size() + " 项证书");
        return matched >= 1 ? 1.0 : (double) studentCerts.size() / 3;
    }
    
    private double checkSoftAbilities(StudentProfileDTO studentProfile, List<String> breakdown) {
        SoftAbilitiesDTO abilities = studentProfile.softAbilities();
        if (abilities == null) {
            breakdown.add("○ 能力评分：未设置软能力评分");
            return 0;
        }
        
        int above70 = 0;
        if (abilities.communication() >= 70) above70++;
        if (abilities.stressTolerance() >= 70) above70++;
        if (abilities.internship() >= 70) above70++;
        if (abilities.learning() >= 70) above70++;
        if (abilities.innovation() >= 70) above70++;
        if (abilities.professionalSkills() >= 70) above70++;
        
        String status = above70 >= 3 ? "✓" : "○";
        breakdown.add(status + " 软能力：" + above70 + "/6 项评分在70分以上");
        return (double) above70 / 6;
    }
    
    private double checkExperiences(StudentProfileDTO studentProfile, List<String> breakdown) {
        int count = 0;
        if (studentProfile.internshipExperiences() != null && !studentProfile.internshipExperiences().isEmpty()) {
            count++;
        }
        if (studentProfile.projectExperiences() != null && !studentProfile.projectExperiences().isEmpty()) {
            count++;
        }
        
        String status = count >= 1 ? "✓" : "○";
        breakdown.add(status + " 经历：拥有 " + count + " 项经历（实习/项目）");
        return (double) count / 2;
    }
    
    private double checkJobPreference(StudentProfileDTO studentProfile, List<String> breakdown) {
        int filled = 0;
        int total = 3;
        if (studentProfile.jobPreference() != null) {
            if (notBlank(studentProfile.jobPreference().expectedPosition())) filled++;
            if (notBlank(studentProfile.jobPreference().expectedSalary())) filled++;
            if (notBlank(studentProfile.jobPreference().expectedCity())) filled++;
        }
        
        String status = filled >= 2 ? "✓" : "○";
        breakdown.add(status + " 求职偏好：" + filled + "/" + total + " 项已设置");
        return (double) filled / total;
    }
    
    private double scoreSkillMatch(StudentProfileDTO studentProfile, JsonNode jobPortrait, List<String> breakdown) {
        List<String> studentSkills = safeList(studentProfile.skills());
        Set<String> normalizedStudentSkills = studentSkills.stream()
                .map(this::normalize)
                .collect(Collectors.toSet());
        
        List<String> jobRequiredSkills = extractJobSkills(jobPortrait);
        long matched = jobRequiredSkills.stream()
                .filter(skill -> normalizedStudentSkills.stream().anyMatch(s -> s.contains(normalize(skill)) || normalize(skill).contains(s)))
                .count();
        
        double score = jobRequiredSkills.isEmpty() ? 35 : (matched * 35.0 / jobRequiredSkills.size());
        breakdown.add("技能匹配度：" + String.format("%.1f", score) + " 分（" + matched + "/" + jobRequiredSkills.size() + "）");
        return score;
    }
    
    private double scoreSkillDepth(StudentProfileDTO studentProfile, JsonNode jobPortrait, List<String> breakdown) {
        List<String> studentSkills = safeList(studentProfile.skills());
        
        double score = Math.min(20, studentSkills.size() * 3.0);
        String detail = studentSkills.size() >= 5 ? "技能丰富" : (studentSkills.size() >= 3 ? "技能够用" : "技能较少");
        breakdown.add("技能深度：" + String.format("%.1f", score) + " 分（" + detail + "）");
        return score;
    }
    
    private double scoreCertificateRelevance(StudentProfileDTO studentProfile, JsonNode jobPortrait, List<String> breakdown) {
        List<String> studentCerts = safeList(studentProfile.certificates());
        List<String> jobCerts = extractJobCertificates(jobPortrait);
        
        long matched = studentCerts.stream()
                .filter(cert -> jobCerts.stream().anyMatch(jc -> normalize(cert).contains(normalize(jc)) || normalize(jc).contains(normalize(cert))))
                .count();
        
        double score = jobCerts.isEmpty() ? 10 : Math.min(10, matched * 5.0 + studentCerts.size() * 2.0);
        breakdown.add("证书相关性：" + String.format("%.1f", score) + " 分");
        return score;
    }
    
    private double scoreSoftAbilities(StudentProfileDTO studentProfile, List<String> breakdown) {
        SoftAbilitiesDTO abilities = studentProfile.softAbilities();
        if (abilities == null) {
            breakdown.add("软能力评分：0 分");
            return 0;
        }
        
        double total = abilities.communication() * 0.20 +
                abilities.stressTolerance() * 0.18 +
                abilities.internship() * 0.15 +
                abilities.professionalSkills() * 0.22 +
                abilities.innovation() * 0.12 +
                abilities.learning() * 0.13;
        
        double score = total * 0.15;
        breakdown.add("软能力：" + String.format("%.1f", score) + " 分");
        return score;
    }
    
    private double scoreExperienceQuality(StudentProfileDTO studentProfile, List<String> breakdown) {
        int internshipCount = studentProfile.internshipExperiences() == null ? 0 : studentProfile.internshipExperiences().size();
        int projectCount = studentProfile.projectExperiences() == null ? 0 : studentProfile.projectExperiences().size();
        
        double score = Math.min(12, internshipCount * 4.0 + projectCount * 3.0);
        breakdown.add("经历质量：" + String.format("%.1f", score) + " 分");
        return score;
    }
    
    private double scoreSkillBreadth(StudentProfileDTO studentProfile, List<String> breakdown) {
        List<String> studentSkills = safeList(studentProfile.skills());
        
        Set<String> skillTypes = studentSkills.stream()
                .map(this::categorizeSkill)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        double score = Math.min(8, skillTypes.size() * 2.0);
        breakdown.add("技能广度：" + String.format("%.1f", score) + " 分（" + skillTypes.size() + " 类）");
        return score;
    }
    
    private List<String> extractJobSkills(JsonNode jobPortrait) {
        List<String> skills = new ArrayList<>();
        JsonNode skillsNode = jobPortrait.path("专业技能");
        if (skillsNode.isObject()) {
            skillsNode.fieldNames().forEachRemaining(field -> {
                if (field.contains("(") || field.contains("（")) {
                    String extracted = extractSkillsFromField(field);
                    if (!extracted.isEmpty()) {
                        skills.add(extracted);
                    }
                } else if (field.length() <= 10) {
                    skills.add(field);
                }
            });
        }
        return skills.stream().distinct().limit(10).toList();
    }
    
    private List<String> extractJobCertificates(JsonNode jobPortrait) {
        List<String> certificates = new ArrayList<>();
        JsonNode certNode = jobPortrait.path("证书要求");
        if (certNode.isObject()) {
            certNode.fieldNames().forEachRemaining(field -> {
                if (!"无".equals(field) && !"无强制证书".equals(field)) {
                    certificates.add(field);
                }
            });
        }
        return certificates;
    }
    
    private String extractSkillsFromField(String field) {
        int start = Math.max(field.indexOf('('), field.indexOf('（'));
        if (start < 0) return "";
        
        int end = Math.max(field.indexOf(')'), field.indexOf('）'));
        if (end < 0) return "";
        
        String content = field.substring(start + 1, end);
        String[] parts = content.split("[/、,，]");
        return parts.length > 0 ? parts[0].trim() : "";
    }
    
    private String categorizeSkill(String skill) {
        String s = normalize(skill);
        if (s.contains("java") || s.contains("python") || s.contains("go") || s.contains("c++")) return "编程语言";
        if (s.contains("spring") || s.contains("vue") || s.contains("react")) return "框架";
        if (s.contains("mysql") || s.contains("redis") || s.contains("oracle")) return "数据库";
        if (s.contains("docker") || s.contains("kubernetes") || s.contains("jenkins")) return "DevOps";
        if (s.contains("git") || s.contains("linux")) return "工具";
        if (s.contains("算法") || s.contains("数据结构")) return "基础能力";
        return "其他";
    }
    
    private JsonNode loadJobPortrait(String jobTitle) {
        if (cachedJobPortraits == null) {
            cachedJobPortraits = loadJobPortraits();
        }
        
        String normalizedTitle = normalize(jobTitle);
        for (Map.Entry<String, JsonNode> entry : cachedJobPortraits.entrySet()) {
            String key = normalize(entry.getKey());
            if (key.contains(normalizedTitle) || normalizedTitle.contains(key)) {
                return entry.getValue();
            }
        }
        
        return cachedJobPortraits.values().stream().findFirst().orElse(null);
    }
    
    private Map<String, JsonNode> loadJobPortraits() {
        Map<String, JsonNode> portraits = new HashMap<>();
        
        if (Files.isDirectory(DEFAULT_PROCESSED_PORTRAIT_DIR)) {
            try (var stream = Files.list(DEFAULT_PROCESSED_PORTRAIT_DIR)) {
                stream
                        .filter(path -> !Files.isDirectory(path))
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                        .forEach(path -> {
                            try {
                                JsonNode portrait = objectMapper.readTree(Files.newInputStream(path));
                                String filename = path.getFileName().toString();
                                String title = filename.endsWith(".json") ? filename.substring(0, filename.length() - 5) : filename;
                                portraits.put(title, portrait);
                            } catch (Exception e) {
                            }
                        });
            } catch (Exception e) {
            }
        }
        
        if (portraits.isEmpty()) {
            try {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                var resources = resolver.getResources(DEFAULT_CLASSPATH_PATTERN);
                for (var resource : resources) {
                    try (InputStream inputStream = resource.getInputStream()) {
                        JsonNode portrait = objectMapper.readTree(inputStream);
                        String filename = resource.getFilename();
                        if (filename != null) {
                            String title = filename.replace("_结构化画像.json", "");
                            portraits.put(title, portrait);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        
        return portraits;
    }
    
    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }
    
    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
    
    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
