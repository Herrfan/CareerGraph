package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.graph.SkillGapAnalysisDTO;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.match.JobMatchDTO;
import com.zust.qyf.careeragent.domain.dto.path.CareerPathNodeDTO;
import com.zust.qyf.careeragent.domain.dto.path.CareerPathResponseDTO;
import com.zust.qyf.careeragent.domain.dto.report.GrowthPlanDTO;
import com.zust.qyf.careeragent.domain.dto.report.GrowthPlanResponseDTO;
import com.zust.qyf.careeragent.domain.dto.student.BasicInfoDTO;
import com.zust.qyf.careeragent.domain.dto.student.JobPreferenceDTO;
import com.zust.qyf.careeragent.domain.dto.student.SoftAbilitiesDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import com.zust.qyf.careeragent.domain.service.MatchScoringService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CareerFamilyPlannerService {
    private final KnowledgeImportService knowledgeImportService;
    private final MatchScoringService matchScoringService;

    public CareerFamilyPlannerService(KnowledgeImportService knowledgeImportService,
                                      MatchScoringService matchScoringService) {
        this.knowledgeImportService = knowledgeImportService;
        this.matchScoringService = matchScoringService;
    }

    public Optional<JobProfileDTO> resolveTargetJob(String jobId, String requestedTitle, StudentProfileDTO studentProfile) {
        if (jobId != null && !jobId.isBlank()) {
            Optional<JobProfileDTO> byId = knowledgeImportService.getJob(jobId);
            if (byId.isPresent()) {
                return byId;
            }
        }

        if (requestedTitle != null && !requestedTitle.isBlank()) {
            String normalized = CareerFamilyMetadata.normalize(requestedTitle);
            Optional<JobProfileDTO> exact = knowledgeImportService.getAllJobs().stream()
                    .filter(job -> CareerFamilyMetadata.normalize(job.title()).equals(normalized)
                            || CareerFamilyMetadata.normalize(job.category()).equals(normalized)
                            || CareerFamilyMetadata.resolve(job.title())
                            .map(family -> family.equals(CareerFamilyMetadata.resolve(requestedTitle).orElse(null)))
                            .orElse(false))
                    .findFirst();
            if (exact.isPresent()) {
                return exact;
            }
        }

        if (studentProfile == null) {
            return Optional.empty();
        }

        List<JobProfileDTO> jobs = knowledgeImportService.getAllJobs();
        if (jobs.isEmpty()) {
            return Optional.empty();
        }
        List<JobMatchDTO> topMatches = matchScoringService.calculateTopMatches(studentProfile, jobs, 1);
        if (!topMatches.isEmpty()) {
            return Optional.of(topMatches.get(0).job());
        }
        return Optional.empty();
    }

    public CareerPathResponseDTO buildCareerPath(JobProfileDTO targetJob, StudentProfileDTO studentProfile) {
        CareerFamilyMetadata.FamilyDefinition family = CareerFamilyMetadata.resolve(targetJob.title())
                .orElseGet(() -> fallbackFamily(targetJob));
        StageAssessment assessment = assessStage(targetJob, studentProfile);
        List<Map<String, Object>> verticalPath = buildVerticalPath(targetJob, family, assessment, studentProfile);
        List<Map<String, Object>> horizontalPath = buildHorizontalPath(family, assessment);
        List<Map<String, Object>> similarFamilies = List.of();
        CareerPathNodeDTO careerTree = buildCareerTree(family.stages(), 0);
        return new CareerPathResponseDTO(careerTree, similarFamilies, verticalPath, horizontalPath);
    }

    public SkillGapAnalysisDTO buildSkillGap(JobProfileDTO targetJob, StudentProfileDTO studentProfile) {
        List<String> requiredSkills = targetJob.requiredSkills() == null ? List.of() : targetJob.requiredSkills();
        List<String> studentSkills = studentProfile == null || studentProfile.skills() == null ? List.of() : studentProfile.skills();
        Set<String> normalizedStudentSkills = studentSkills.stream()
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : requiredSkills) {
            String normalizedRequired = normalize(skill);
            boolean present = normalizedStudentSkills.stream()
                    .anyMatch(student -> student.contains(normalizedRequired) || normalizedRequired.contains(student));
            if (present) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        double score = requiredSkills.isEmpty() ? 100.0 : Math.round(matched.size() * 1000.0 / requiredSkills.size()) / 10.0;
        String summary;
        if (missing.isEmpty()) {
            summary = "当前画像已经覆盖该岗位族的核心技能要求，下一步更适合补强项目复杂度与结果表达。";
        } else if (matched.isEmpty()) {
            summary = "当前画像与目标岗位族仍有明显差距，优先补齐基础技能、实操案例和稳定输出能力。";
        } else {
            summary = "当前已经具备部分岗位基础，但还需要围绕关键短板持续补齐，才能进入更高阶段。";
        }

        return new SkillGapAnalysisDTO(targetJob.title(), matched, missing, score, summary);
    }

    public GrowthPlanResponseDTO buildGrowthPlan(JobProfileDTO targetJob, StudentProfileDTO studentProfile) {
        CareerFamilyMetadata.FamilyDefinition family = CareerFamilyMetadata.resolve(targetJob.title())
                .orElseGet(() -> fallbackFamily(targetJob));
        StageAssessment assessment = assessStage(targetJob, studentProfile);
        SkillGapAnalysisDTO skillGap = buildSkillGap(targetJob, studentProfile);

        CareerFamilyMetadata.StageDefinition currentStage = family.stages().get(assessment.stageIndex());
        CareerFamilyMetadata.StageDefinition nextStage = family.stages().get(Math.min(assessment.stageIndex() + 1, family.stages().size() - 1));

        GrowthPlanDTO shortTerm = new GrowthPlanDTO(
                List.of(
                        learningItem("先补岗位族核心技能", buildSkillTarget(skillGap.missingSkills(), family.coreSkills(), 3)),
                        learningItem("补齐阶段短板证据", "围绕 " + currentStage.roleName() + " 阶段补 1 个可验证项目或实习成果"),
                        learningItem("建立表达模板", "整理简历、项目复盘和面试话术，让已有能力可以被清楚证明")
                ),
                List.of(
                        practiceItem("阶段目标", "围绕 " + currentStage.roleName() + " 能独立完成的典型任务做一次完整演练"),
                        practiceItem("交付要求", "至少产出 1 份代码/方案/测试/实施/项目文档等可展示材料")
                ),
                Map.of(
                        "current_stage", currentStage.roleName(),
                        "readiness_score", assessment.readinessScore(),
                        "focus", "先站稳当前阶段，再向下一阶段推进"
                )
        );

        GrowthPlanDTO midTerm = new GrowthPlanDTO(
                List.of(
                        learningItem("对齐下一阶段能力", "围绕 " + nextStage.roleName() + " 补齐复杂任务、协同推进和问题闭环能力"),
                        learningItem("形成岗位族作品集", "把项目、实习、竞赛或研究成果整理成能证明成长轨迹的作品集"),
                        learningItem("沉淀方法论", "把常用工具链、流程、排错方法和经验总结固定下来")
                ),
                List.of(
                        practiceItem("目标阶段", "争取承担 1 次更完整的模块/课题/项目责任，向 " + nextStage.roleName() + " 靠近"),
                        practiceItem("验证方式", "用复盘、汇报、展示或阶段答辩证明自己已经具备更高层级的稳定输出")
                ),
                Map.of(
                        "target_stage", nextStage.roleName(),
                        "next_gap_count", skillGap.missingSkills().size(),
                        "focus", "从当前可胜任走向更高阶段的持续跃升"
                )
        );

        return new GrowthPlanResponseDTO(true, shortTerm, midTerm);
    }

    private CareerFamilyMetadata.FamilyDefinition fallbackFamily(JobProfileDTO targetJob) {
        return CareerFamilyMetadata.resolve(targetJob.category())
                .orElseGet(() -> CareerFamilyMetadata.allFamilies().stream()
                        .filter(item -> CareerFamilyMetadata.normalize(item.displayName())
                                .equals(CareerFamilyMetadata.normalize(targetJob.title())))
                        .findFirst()
                        .orElse(CareerFamilyMetadata.allFamilies().get(0)));
    }

    public Map<String, Object> summarizeCurrentStage(JobProfileDTO targetJob, StudentProfileDTO studentProfile) {
        CareerFamilyMetadata.FamilyDefinition family = CareerFamilyMetadata.resolve(targetJob.title())
                .orElseGet(() -> fallbackFamily(targetJob));
        StageAssessment assessment = assessStage(targetJob, studentProfile);
        CareerFamilyMetadata.StageDefinition stage = family.stages().get(assessment.stageIndex());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("family", family.displayName());
        result.put("phaseName", stage.phaseName());
        result.put("jobTitle", stage.roleName());
        result.put("reason", assessment.narrative());
        result.put("readinessScore", assessment.readinessScore());
        return result;
    }

    private StageAssessment assessStage(JobProfileDTO targetJob, StudentProfileDTO studentProfile) {
        if (studentProfile == null) {
            return new StageAssessment(0, 0.0, "未提供学生画像，默认从岗位族的起步阶段展示纵向路径。");
        }

        double score = 0.0;
        BasicInfoDTO basicInfo = studentProfile.basicInfo();
        JobPreferenceDTO preference = studentProfile.jobPreference();
        SoftAbilitiesDTO softAbilities = studentProfile.softAbilities();
        CareerFamilyMetadata.FamilyDefinition family = CareerFamilyMetadata.resolve(targetJob.title())
                .orElseGet(() -> fallbackFamily(targetJob));

        score += educationScore(basicInfo) * 0.18;
        score += majorScore(basicInfo, family) * 0.12;
        score += skillCoverageScore(studentProfile.skills(), targetJob.requiredSkills()) * 0.30;
        score += Math.min(100, safeSize(studentProfile.skills()) * 8.0) * 0.08;
        score += certificateScore(studentProfile.certificates(), family) * 0.08;
        score += softSkillScore(softAbilities) * 0.18;
        score += internshipScore(softAbilities) * 0.06;
        score += preferenceScore(preference, targetJob) * 0.08;

        double readiness = round(Math.max(0, Math.min(100, score)));
        int stageIndex;
        if (readiness < 45) {
            stageIndex = 0;
        } else if (readiness < 65) {
            stageIndex = 1;
        } else if (readiness < 82) {
            stageIndex = 2;
        } else {
            stageIndex = 3;
        }

        String narrative = switch (stageIndex) {
            case 0 -> "从学历背景、技能证据和实操经历看，当前更适合先站稳岗位族的起步阶段。";
            case 1 -> "当前已经具备基础岗位胜任力，适合向独立交付和稳定输出阶段推进。";
            case 2 -> "当前画像已经接近岗位族的骨干层，下一步重点是复杂任务承接与方法沉淀。";
            default -> "当前画像已经具备较强综合基础，可以朝更高层级的统筹、架构或引领角色发展。";
        };
        return new StageAssessment(stageIndex, readiness, narrative);
    }

    private double educationScore(BasicInfoDTO basicInfo) {
        if (basicInfo == null || basicInfo.education() == null) {
            return 45;
        }
        String education = basicInfo.education().toLowerCase(Locale.ROOT);
        if (education.contains("博士")) return 100;
        if (education.contains("硕")) return 88;
        if (education.contains("本")) return 76;
        if (education.contains("专")) return 60;
        return 45;
    }

    private double majorScore(BasicInfoDTO basicInfo, CareerFamilyMetadata.FamilyDefinition family) {
        if (basicInfo == null || basicInfo.major() == null) {
            return 40;
        }
        String major = normalize(basicInfo.major());
        if (containsAny(major, "计算机", "软件", "信息", "电子", "通信", "自动化", "人工智能", "数据")) {
            return 90;
        }
        if (family.aliases().stream().map(this::normalize).anyMatch(major::contains)) {
            return 78;
        }
        if (containsAny(major, "管理", "数学", "物理")) {
            return 60;
        }
        return 40;
    }

    private double skillCoverageScore(List<String> studentSkills, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 70;
        }
        Set<String> normalizedStudentSkills = new LinkedHashSet<>();
        if (studentSkills != null) {
            studentSkills.stream().map(this::normalize).filter(value -> !value.isBlank()).forEach(normalizedStudentSkills::add);
        }
        long matched = requiredSkills.stream()
                .map(this::normalize)
                .filter(required -> normalizedStudentSkills.stream()
                        .anyMatch(student -> student.contains(required) || required.contains(student)))
                .count();
        return matched * 100.0 / requiredSkills.size();
    }

    private double certificateScore(List<String> certificates, CareerFamilyMetadata.FamilyDefinition family) {
        if (certificates == null || certificates.isEmpty()) {
            return 35;
        }
        Set<String> normalized = certificates.stream().map(this::normalize).collect(LinkedHashSet::new, Set::add, Set::addAll);
        boolean matched = family.recommendedCertificates().stream()
                .map(this::normalize)
                .anyMatch(cert -> normalized.stream().anyMatch(item -> item.contains(cert) || cert.contains(item)));
        return matched ? 90 : Math.min(82, 45 + certificates.size() * 12.0);
    }

    private double softSkillScore(SoftAbilitiesDTO abilities) {
        if (abilities == null) {
            return 55;
        }
        return abilities.professionalSkills() * 0.36
                + abilities.learning() * 0.26
                + abilities.communication() * 0.20
                + abilities.stressTolerance() * 0.18;
    }

    private double internshipScore(SoftAbilitiesDTO abilities) {
        if (abilities == null) {
            return 40;
        }
        return Math.max(0, Math.min(100, abilities.internship()));
    }

    private double preferenceScore(JobPreferenceDTO preference, JobProfileDTO targetJob) {
        if (preference == null) {
            return 30;
        }
        double score = 30;
        String expectedPosition = normalize(preference.expectedPosition());
        String target = normalize(targetJob.title());
        if (!expectedPosition.isBlank()) {
            score += expectedPosition.equals(target) || target.contains(expectedPosition) || expectedPosition.contains(target) ? 45 : 8;
        }
        if (preference.expectedSalary() != null && !preference.expectedSalary().isBlank()) {
            score += 12;
        }
        if (preference.expectedCity() != null && !preference.expectedCity().isBlank()) {
            score += 13;
        }
        return Math.min(100, score);
    }

    private List<Map<String, Object>> buildVerticalPath(JobProfileDTO targetJob,
                                                        CareerFamilyMetadata.FamilyDefinition family,
                                                        StageAssessment assessment,
                                                        StudentProfileDTO studentProfile) {
        List<Map<String, Object>> result = new ArrayList<>();
        int baseSalary = parseSalaryMidpoint(targetJob.salaryRange());
        int[] defaultSalaries = new int[]{7000, 11000, 17000, 25000};
        double[] factors = new double[]{0.60, 0.90, 1.18, 1.50};

        for (int index = 0; index < family.stages().size(); index++) {
            CareerFamilyMetadata.StageDefinition stage = family.stages().get(index);
            int referenceSalary = baseSalary > 0
                    ? Math.max((int) Math.round(baseSalary * factors[index]), defaultSalaries[index])
                    : defaultSalaries[index];
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stage", index + 1);
            item.put("phaseName", stage.phaseName());
            item.put("jobTitle", stage.roleName());
            item.put("category", family.displayName());
            item.put("city", targetJob.city());
            item.put("salaryRange", formatSalary(referenceSalary));
            item.put("referenceSalary", formatSalary(referenceSalary));
            item.put("referenceSalaryMid", referenceSalary);
            item.put("jobDescription", stage.summary());
            item.put("requiredSkills", stageSkills(targetJob.requiredSkills(), family.coreSkills(), index));
            item.put("promotionRelation", relationText(family, stage, assessment, index, studentProfile != null));
            item.put("reason", relationText(family, stage, assessment, index, studentProfile != null));
            item.put("stageStatus", stageStatus(index, assessment.stageIndex(), studentProfile != null));
            item.put("readinessScore", assessment.readinessScore());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildHorizontalPath(CareerFamilyMetadata.FamilyDefinition family, StageAssessment assessment) {
        List<Map<String, Object>> paths = family.relatedFamilies().stream()
                .map(CareerFamilyMetadata::resolve)
                .flatMap(Optional::stream)
                .map(related -> {
                    int targetStageIndex = related.stages().size() > 1 ? 1 : 0;
                    CareerFamilyMetadata.StageDefinition targetStage = related.stages().get(targetStageIndex);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("jobTitle", targetStage.roleName());
                    item.put("phaseName", targetStage.phaseName());
                    item.put("salaryRange", related.displayName());
                    item.put("jobDescription", targetStage.summary());
                    item.put("switchReason", buildTransitionReason(family, related, assessment));
                    return item;
                })
                .toList();

        if (paths.isEmpty()) {
            return List.of();
        }

        Map<String, Object> current = new LinkedHashMap<>();
        current.put("jobTitle", family.displayName());
        current.put("salaryRange", family.stages().get(assessment.stageIndex()).roleName());
        current.put("jobDescription", family.overview());
        current.put("relationshipType", "current");
        current.put("switchReason", "以下方向是基于当前岗位族能力结构给出的延展路线，重点看能力迁移和下一步可承接的岗位阶段。");
        current.put("paths", paths);
        return List.of(current);
    }

    private CareerPathNodeDTO buildCareerTree(List<CareerFamilyMetadata.StageDefinition> stages, int index) {
        if (index >= stages.size()) {
            return null;
        }
        CareerPathNodeDTO next = buildCareerTree(stages, index + 1);
        List<CareerPathNodeDTO> paths = next == null ? List.of() : List.of(next);
        return new CareerPathNodeDTO(stages.get(index).roleName(), index + 1, paths);
    }

    private String relationText(CareerFamilyMetadata.FamilyDefinition family,
                                CareerFamilyMetadata.StageDefinition stage,
                                StageAssessment assessment,
                                int index,
                                boolean hasProfile) {
        if (!hasProfile) {
            return index == 0
                    ? "这是 " + family.displayName() + " 的起点阶段，适合先理解基本职责、工具链和交付节奏。"
                    : "在完成上一阶段能力积累后，可以继续向“" + stage.roleName() + "”推进。";
        }
        if (index < assessment.stageIndex()) {
            return "该阶段强调基础能力打底与稳定执行，是进入更高阶段前必须站稳的能力层。";
        }
        if (index == assessment.stageIndex()) {
            return assessment.narrative();
        }
        if (index == assessment.stageIndex() + 1) {
            return "下一步应重点把能力抬升到“" + stage.roleName() + "”所要求的独立交付、协同推进或复杂问题处理层面。";
        }
        return "这是更高层级的发展方向，通常要求更强的统筹、影响力与方法沉淀能力。";
    }

    private String buildTransitionReason(CareerFamilyMetadata.FamilyDefinition source,
                                         CareerFamilyMetadata.FamilyDefinition target,
                                         StageAssessment assessment) {
        String sourceName = source.displayName();
        String targetName = target.displayName();

        if ("Java开发".equals(sourceName) && "前端开发".equals(targetName)) {
            return "如果你已经具备接口理解、业务建模和工程协同能力，补齐页面交互与组件化经验后，可以转向前端开发工程师阶段。";
        }
        if ("Java开发".equals(sourceName) && ("测试工程师".equals(targetName) || "软件测试".equals(targetName))) {
            return "如果你更擅长接口理解、缺陷定位和质量把关，可以把开发经验迁移到自动化测试或测试工程师阶段。";
        }
        if ("前端开发".equals(sourceName) && "产品专员/助理".equals(targetName)) {
            return "前端阶段积累的交互理解和需求拆解能力，适合继续往产品助理或产品专员方向过渡。";
        }
        if ("测试工程师".equals(sourceName) && "项目经理/主管".equals(targetName)) {
            return "如果你已经能稳定推进缺陷闭环和跨团队协作，进一步补齐计划推进与资源协调能力后，可向项目经理路径转化。";
        }
        if ("实施工程师".equals(sourceName) && "技术支持工程师".equals(targetName)) {
            return "实施阶段形成的客户现场经验和问题闭环能力，通常可以平滑转入技术支持工程师阶段。";
        }
        if ("技术支持工程师".equals(sourceName) && "实施工程师".equals(targetName)) {
            return "如果你已经有稳定的客户问题诊断能力，补齐部署上线与项目交付流程后，可以转向实施工程师阶段。";
        }
        if ("产品专员/助理".equals(sourceName) && "项目经理/主管".equals(targetName)) {
            return "当你已经能稳定推动需求落地和跨团队协作后，可以继续提升进度统筹与资源协调，向项目经理阶段靠近。";
        }
        if ("科研人员".equals(sourceName) && "C/C++开发".equals(targetName)) {
            return "如果你的研究方向更偏底层实现、性能优化或系统建模，可以把研究能力转成 C/C++ 开发阶段的工程落地能力。";
        }

        String currentStage = source.stages().get(assessment.stageIndex()).roleName();
        String targetStage = target.stages().size() > 1 ? target.stages().get(1).roleName() : target.stages().get(0).roleName();
        return "你当前若已接近“" + currentStage + "”的稳定输出水平，可把已有的通用能力迁移到“" + targetStage + "”所要求的职责场景中。";
    }

    private String stageStatus(int index, int currentIndex, boolean hasProfile) {
        if (!hasProfile) {
            return index == 0 ? "entry" : "future";
        }
        if (index < currentIndex) {
            return "foundation";
        }
        if (index == currentIndex) {
            return "current";
        }
        if (index == currentIndex + 1) {
            return "next";
        }
        return "future";
    }

    private List<String> stageSkills(List<String> requiredSkills, List<String> coreSkills, int stageIndex) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (requiredSkills != null) {
            int start = Math.min(stageIndex * 2, requiredSkills.size());
            requiredSkills.stream().skip(start).limit(4).forEach(result::add);
            requiredSkills.stream().limit(6).forEach(result::add);
        }
        coreSkills.stream().limit(6).forEach(result::add);
        return result.stream().limit(6).toList();
    }

    private Map<String, Object> learningItem(String title, String detail) {
        return Map.of("title", title, "detail", detail);
    }

    private Map<String, Object> practiceItem(String title, String detail) {
        return Map.of("title", title, "detail", detail);
    }

    private String buildSkillTarget(List<String> missingSkills, List<String> defaults, int limit) {
        List<String> source = missingSkills == null || missingSkills.isEmpty() ? defaults : missingSkills;
        return source.stream().limit(limit).reduce((left, right) -> left + "、" + right).orElse("岗位基础能力");
    }

    private String formatSalary(int monthlySalary) {
        if (monthlySalary <= 0) {
            return "薪资面议";
        }
        int lower = Math.max(4, (int) Math.floor(monthlySalary * 0.85 / 1000.0));
        int upper = Math.max(lower + 2, (int) Math.ceil(monthlySalary * 1.15 / 1000.0));
        return lower + "K-" + upper + "K";
    }

    private int parseSalaryMidpoint(String salaryText) {
        if (salaryText == null || salaryText.isBlank()) {
            return -1;
        }
        String normalized = salaryText.toLowerCase(Locale.ROOT)
                .replace("k", "000")
                .replace("万", "0000")
                .replace("千", "000");
        List<Integer> numbers = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(normalized);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
            if (numbers.size() >= 2) {
                break;
            }
        }
        if (numbers.isEmpty()) {
            return -1;
        }
        return numbers.size() == 1 ? numbers.get(0) : (numbers.get(0) + numbers.get(1)) / 2;
    }

    private int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return CareerFamilyMetadata.normalize(value);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record StageAssessment(int stageIndex, double readinessScore, String narrative) {
    }
}
