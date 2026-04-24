package com.zust.qyf.careeragent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.ResumeDTO;
import com.zust.qyf.careeragent.domain.dto.graph.SkillGapAnalysisDTO;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.match.MatchRequestDTO;
import com.zust.qyf.careeragent.domain.dto.match.MatchResultDTO;
import com.zust.qyf.careeragent.domain.dto.path.CareerPathResponseDTO;
import com.zust.qyf.careeragent.domain.dto.report.CompletenessCheckResponseDTO;
import com.zust.qyf.careeragent.domain.dto.report.ExportRequestDTO;
import com.zust.qyf.careeragent.domain.dto.report.GeneratedReportDTO;
import com.zust.qyf.careeragent.domain.dto.report.GrowthPlanDTO;
import com.zust.qyf.careeragent.domain.dto.report.GrowthPlanResponseDTO;
import com.zust.qyf.careeragent.domain.dto.report.PolishReportRequestDTO;
import com.zust.qyf.careeragent.domain.dto.report.ReportExportResponseDTO;
import com.zust.qyf.careeragent.domain.dto.report.ReportGenerateRequestDTO;
import com.zust.qyf.careeragent.domain.dto.report.ReportSnapshotDTO;
import com.zust.qyf.careeragent.domain.dto.report.SaveReportSnapshotRequestDTO;
import com.zust.qyf.careeragent.domain.dto.student.AbilityDescriptionsDTO;
import com.zust.qyf.careeragent.domain.dto.student.BasicInfoDTO;
import com.zust.qyf.careeragent.domain.dto.student.InternshipExperienceDTO;
import com.zust.qyf.careeragent.domain.dto.student.JobPreferenceDTO;
import com.zust.qyf.careeragent.domain.dto.student.ProjectExperienceDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import com.zust.qyf.careeragent.domain.dto.report.ProfileScoringDTO;
import com.zust.qyf.careeragent.domain.service.ProfileScoringService;
import com.zust.qyf.careeragent.utils.PromptUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReportApplicationService {
    private final ReportSnapshotService reportSnapshotService;
    private final CareerFamilyPlannerService careerFamilyPlannerService;
    private final MatchApplicationService matchApplicationService;
    private final ResumeService resumeService;
    private final PromptUtil promptUtil;
    private final ChatClient pureChatClient;
    private final ObjectMapper objectMapper;
    private final ProfileScoringService profileScoringService;

    public ReportApplicationService(ReportSnapshotService reportSnapshotService,
                                    CareerFamilyPlannerService careerFamilyPlannerService,
                                    MatchApplicationService matchApplicationService,
                                    ResumeService resumeService,
                                    PromptUtil promptUtil,
                                    @Qualifier("pureChatClient") ChatClient pureChatClient,
                                    ObjectMapper objectMapper,
                                    ProfileScoringService profileScoringService) {
        this.reportSnapshotService = reportSnapshotService;
        this.careerFamilyPlannerService = careerFamilyPlannerService;
        this.matchApplicationService = matchApplicationService;
        this.resumeService = resumeService;
        this.promptUtil = promptUtil;
        this.pureChatClient = pureChatClient;
        this.objectMapper = objectMapper;
        this.profileScoringService = profileScoringService;
    }

    public ProfileScoringDTO scoreProfile(StudentProfileDTO studentProfile, String targetJob) {
        return profileScoringService.scoreProfile(studentProfile, targetJob);
    }

    public GeneratedReportDTO generateReport(ReportGenerateRequestDTO request, Long userId) {
        StudentProfileDTO studentProfile = request.studentProfile();
        if (studentProfile == null) {
            throw new IllegalArgumentException("studentProfile must not be null");
        }

        JobProfileDTO targetJob = resolveTargetJob(request.jobId(), request.targetJob(), studentProfile);
        MatchResultDTO matchResult = matchApplicationService.calculateMatch(
                new MatchRequestDTO(studentProfile, targetJob.jobId(), null, 1)
        );
        CareerPathResponseDTO careerPath = careerFamilyPlannerService.buildCareerPath(targetJob, studentProfile);
        SkillGapAnalysisDTO skillGap = careerFamilyPlannerService.buildSkillGap(targetJob, studentProfile);
        ResumeDTO resumeData = loadResumeData(userId);
        GrowthPlanResponseDTO growthPlan;
        String markdown;
        try {
            growthPlan = generateGrowthPlanWithKimi(studentProfile, targetJob, resumeData);
            markdown = generateReportWithKimi(
                    studentProfile,
                    targetJob,
                    matchResult,
                    careerPath,
                    skillGap,
                    growthPlan,
                    resumeData
            );
        } catch (Exception e) {
            throw new IllegalStateException("Kimi report generation failed", e);
        }

        if (userId != null) {
            reportSnapshotService.saveOrUpdate(
                    userId,
                    safeStudentId(studentProfile),
                    targetJob.title(),
                    targetJob.jobId(),
                    targetJob.title(),
                    markdown,
                    growthPlan
            );
        }
        return new GeneratedReportDTO(true, markdown);
    }

    public Map<String, Object> polishReport(PolishReportRequestDTO request) {
        if (request.reportContent() == null || request.reportContent().isEmpty()) {
            throw new IllegalArgumentException("reportContent must not be empty");
        }

        String sourceText = stringifyMarkdown(request.reportContent().get("markdown_content"));
        if (blank(sourceText)) {
            throw new IllegalArgumentException("markdown_content must not be empty");
        }

        String polished = sourceText.replaceAll("\n{3,}", "\n\n").trim();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("polished_content", polished);
        result.put("original_content", sourceText);
        result.put("modifications", List.of(Map.of(
                "section", blank(request.polishScope()) ? "full_report" : request.polishScope(),
                "changes", "已统一段落节奏和表达方式，未引入新的外部事实。"
        )));
        return result;
    }

    public CompletenessCheckResponseDTO checkCompleteness(Map<String, Object> reportContent) {
        Map<String, Object> content = reportContent == null ? Map.of() : reportContent;
        List<String> completedItems = new ArrayList<>();
        List<String> missingItems = new ArrayList<>();
        List<String> suggestedItems = List.of(
                "学生画像总结",
                "当前更适合的阶段岗位",
                "为什么是这个阶段",
                "为什么还不是下一阶段",
                "项目与实践经历分析",
                "下一步行动计划"
        );

        checkField(content.get("target_job"), "目标岗位族", completedItems, missingItems);
        checkField(content.get("matched_job"), "岗位画像", completedItems, missingItems);
        checkField(content.get("growth_plan"), "下一步行动计划", completedItems, missingItems);

        Object studentProfile = content.get("student_profile");
        if (studentProfile instanceof Map<?, ?> profileMap && !profileMap.isEmpty()) {
            completedItems.add("学生画像");
        } else {
            missingItems.add("学生画像");
        }

        String markdown = stringifyMarkdown(content.get("markdown_content"));
        if (!blank(markdown) && markdown.length() >= 500) {
            completedItems.add("职业报告正文");
        } else {
            missingItems.add("职业报告正文");
        }

        int total = completedItems.size() + missingItems.size();
        double completeness = total == 0 ? 0.0 : Math.round(completedItems.size() * 1000.0 / total) / 10.0;
        return new CompletenessCheckResponseDTO(true, completeness, completedItems, missingItems, suggestedItems);
    }

    public GrowthPlanResponseDTO generateGrowthPlan(StudentProfileDTO studentProfile, String targetJob, Long userId) {
        JobProfileDTO target = resolveTargetJob(null, targetJob, studentProfile);
        ResumeDTO resumeData = loadResumeData(userId);
        GrowthPlanResponseDTO result;
        try {
            result = generateGrowthPlanWithKimi(studentProfile, target, resumeData);
        } catch (Exception e) {
            throw new IllegalStateException("Kimi growth plan generation failed", e);
        }

        if (userId != null) {
            reportSnapshotService.saveOrUpdate(
                    userId,
                    safeStudentId(studentProfile),
                    target.title(),
                    target.jobId(),
                    target.title(),
                    null,
                    result
            );
        }
        return result;
    }

    public ReportExportResponseDTO exportReport(ExportRequestDTO request) {
        Map<String, Object> reportContent = request.reportContent() == null ? Map.of() : request.reportContent();
        String studentName = extractStudentName(reportContent);
        String targetJob = String.valueOf(reportContent.getOrDefault("target_job", "unknown_job"));
        String format = blank(request.exportFormat()) ? "md" : request.exportFormat();
        String filename = "career_report_" + studentName + "_" + targetJob + "_" + LocalDate.now() + "." + format;
        String markdown = stringifyMarkdown(reportContent.get("markdown_content"));
        String content = "html".equalsIgnoreCase(format) ? buildHtmlReport(markdown, request) : markdown;
        List<String> availableSections = request.exportSections() == null || request.exportSections().isEmpty()
                ? List.of("full_report")
                : request.exportSections();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("page_style", request.pageStyle());
        metadata.put("header_text", request.headerText());
        metadata.put("footer_text", request.footerText());
        metadata.put("show_page_numbers", request.showPageNumbers());
        metadata.put("watermark", request.watermark());
        metadata.put("target_job", targetJob);
        metadata.put("student_name", studentName);

        return new ReportExportResponseDTO(
                true,
                UUID.randomUUID().toString(),
                filename,
                format,
                content,
                resolveContentType(format),
                availableSections,
                metadata
        );
    }

    public Optional<ReportSnapshotDTO> getLatestSnapshot(Long userId, String targetJob) {
        return reportSnapshotService.getLatest(userId, targetJob);
    }

    public List<ReportSnapshotDTO> listSnapshots(Long userId) {
        return reportSnapshotService.listByUser(userId);
    }

    public ReportSnapshotDTO saveSnapshot(Long userId, SaveReportSnapshotRequestDTO request) {
        if (request.studentProfile() == null || blank(request.studentProfile().studentId())) {
            throw new IllegalArgumentException("student profile with studentId is required");
        }
        String actualTargetJob = blank(request.targetJob())
                ? resolveTargetJob(null, null, request.studentProfile()).title()
                : request.targetJob();
        return reportSnapshotService.saveOrUpdate(
                userId,
                request.studentProfile().studentId(),
                actualTargetJob,
                request.matchedJobId(),
                request.matchedJobTitle(),
                request.markdownContent(),
                request.growthPlan()
        );
    }

    private GrowthPlanResponseDTO generateGrowthPlanWithKimi(StudentProfileDTO studentProfile,
                                                             JobProfileDTO targetJob,
                                                             ResumeDTO resumeData) throws Exception {
        String systemPrompt = promptUtil.getPrompt("growth_plan_json.txt");
        String userPrompt = """
                [学生画像总结]
                %s

                [简历与实践证据总结]
                %s

                [目标岗位摘要]
                %s

                [当前阶段判断]
                %s

                [技能差距]
                %s

                [当前最不该浪费时间做什么]
                %s

                [已有项目与实习简表]
                %s
                """.formatted(
                buildStudentSummaryContext(studentProfile, resumeData),
                buildResumeEvidenceSummary(studentProfile, resumeData),
                buildTargetJobSummary(targetJob),
                buildCurrentStageSummary(targetJob, studentProfile),
                buildSkillGapSummary(careerFamilyPlannerService.buildSkillGap(targetJob, studentProfile)),
                buildDoNotWasteTimeContext(studentProfile, targetJob, resumeData),
                buildCompactResumeEvidence(studentProfile, resumeData)
        );

        String content = pureChatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(ChatOptions.builder().temperature(0.4).maxTokens(2200).build())
                .call()
                .content();

        return objectMapper.readValue(stripCodeFence(content), GrowthPlanResponseDTO.class);
    }

    private String generateReportWithKimi(StudentProfileDTO studentProfile,
                                          JobProfileDTO targetJob,
                                          MatchResultDTO matchResult,
                                          CareerPathResponseDTO careerPath,
                                          SkillGapAnalysisDTO skillGap,
                                          GrowthPlanResponseDTO growthPlan,
                                          ResumeDTO resumeData) throws Exception {
        String systemPrompt = promptUtil.getPrompt("report_generate.txt");
        String userPrompt = """
                [学生画像总结]
                %s

                [简历与实践证据总结]
                %s

                [目标岗位摘要]
                %s

                [当前阶段判断]
                %s

                [匹配结果摘要]
                %s

                [技能差距]
                %s

                [职业路径摘要]
                %s

                [下一步行动计划摘要]
                %s

                [已有项目与实习简表]
                %s
                """.formatted(
                buildStudentSummaryContext(studentProfile, resumeData),
                buildResumeEvidenceSummary(studentProfile, resumeData),
                buildTargetJobSummary(targetJob),
                buildCurrentStageSummary(targetJob, studentProfile),
                buildMatchSummary(matchResult),
                buildSkillGapSummary(skillGap),
                buildCareerPathSummary(careerPath),
                buildGrowthPlanSummary(growthPlan),
                buildCompactResumeEvidence(studentProfile, resumeData)
        );

        return pureChatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(ChatOptions.builder().temperature(0.5).maxTokens(3200).build())
                .call()
                .content()
                .trim();
    }

    private JobProfileDTO resolveTargetJob(String jobId, String targetJob, StudentProfileDTO studentProfile) {
        return careerFamilyPlannerService.resolveTargetJob(jobId, targetJob, studentProfile)
                .orElseThrow(() -> new IllegalArgumentException("unable to resolve target job"));
    }

    private ResumeDTO loadResumeData(Long userId) {
        String resumeUserId = userId == null ? "1" : String.valueOf(userId);
        return resumeService.getResumeData(resumeUserId);
    }

    private Map<String, Object> resumeProjectContext(ResumeDTO resumeData) {
        if (resumeData == null) {
            return Map.of(
                    "has_resume", false,
                    "project_count", 0,
                    "projects", List.of(),
                    "work_experience", List.of()
            );
        }
        return Map.of(
                "has_resume", true,
                "project_count", resumeData.projects() == null ? 0 : resumeData.projects().size(),
                "projects", resumeData.projects() == null ? List.of() : resumeData.projects(),
                "work_experience", resumeData.workExperience() == null ? List.of() : resumeData.workExperience(),
                "resume_score", resumeData.score()
        );
    }

    private String buildStudentSummaryContext(StudentProfileDTO studentProfile, ResumeDTO resumeData) {
        List<String> lines = new ArrayList<>();
        BasicInfoDTO basicInfo = studentProfile == null ? null : studentProfile.basicInfo();
        JobPreferenceDTO preference = studentProfile == null ? null : studentProfile.jobPreference();
        AbilityDescriptionsDTO descriptions = studentProfile == null ? null : studentProfile.abilityDescriptions();

        if (basicInfo != null) {
            lines.add("基础背景：" + safeText(basicInfo.school(), "学校未填") + " / "
                    + safeText(basicInfo.major(), "专业未填") + " / "
                    + safeText(basicInfo.education(), "学历未填"));
        }

        lines.add("技术线索：" + joinList(studentProfile == null ? List.of() : studentProfile.skills()));
        lines.add("证书线索：" + joinList(studentProfile == null ? List.of() : studentProfile.certificates()));

        if (descriptions != null) {
            lines.add("硬实力描述：" + safeText(descriptions.professionalSkill(), "未单独提供硬实力描述"));
            lines.add("软实力描述：" + buildSoftSkillSummary(descriptions));
        }

        if (preference != null) {
            lines.add("求职意向：" + safeText(preference.expectedPosition(), "岗位未填")
                    + " / " + safeText(preference.expectedCity(), "城市未填")
                    + " / " + safeText(preference.expectedSalary(), "薪资未填"));
        }

        int projectCount = resumeData != null
                ? (resumeData.projects() == null ? 0 : resumeData.projects().size())
                : projectExperiences(studentProfile).size();
        int workCount = resumeData != null
                ? (resumeData.workExperience() == null ? 0 : resumeData.workExperience().size())
                : internshipExperiences(studentProfile).size();
        lines.add("项目与实践概况：项目数=" + projectCount
                + "；实习/工作经历数=" + workCount
                + "；可重点表扬的线索=" + buildCommendableEvidenceSummary(studentProfile, resumeData));

        if (resumeData != null && resumeData.score() != null) {
            lines.add("简历总评：" + safeText(resumeData.score().comment(), "未提供")
                    + "；优势=" + joinList(resumeData.score().advantages())
                    + "；短板=" + joinList(resumeData.score().disadvantages()));
            lines.add("维度分：技术=" + safeNumber(resumeData.score().technical())
                    + "；证书=" + safeNumber(resumeData.score().certificatesScore())
                    + "；创新/进取心=" + safeNumber(resumeData.score().innovation())
                    + "；学习能力=" + safeNumber(resumeData.score().learning())
                    + "；沟通=" + safeNumber(resumeData.score().communication())
                    + "；实践=" + safeNumber(resumeData.score().internship()));
        }

        return String.join("\n", lines);
    }

    private String buildResumeEvidenceSummary(StudentProfileDTO studentProfile, ResumeDTO resumeData) {
        if (resumeData == null) {
            return buildProfileEvidenceSummary(studentProfile);
        }

        List<String> parts = new ArrayList<>();

        if (resumeData.projects() != null && !resumeData.projects().isEmpty()) {
            List<String> projectLines = new ArrayList<>();
            resumeData.projects().forEach(project -> projectLines.add(
                    safeText(project.name(), "未命名项目")
                            + " | 角色=" + safeText(project.role(), "未写角色")
                            + " | 技术=" + joinList(project.techStacks())
                            + " | 描述=" + safeText(project.description(), "未写描述")
                            + " | 亮点=" + safeText(project.highlight(), "未写亮点")
            ));
            parts.add("项目证据：\n- " + String.join("\n- ", projectLines));
        } else {
            parts.add("项目证据：当前未提供明确项目列表。");
        }

        if (resumeData.workExperience() != null && !resumeData.workExperience().isEmpty()) {
            List<String> workLines = new ArrayList<>();
            resumeData.workExperience().forEach(work -> workLines.add(
                    safeText(work.company(), "未写单位")
                            + " | 岗位=" + safeText(work.position(), "未写岗位")
                            + " | 成果=" + safeText(work.achievement(), "未写成果")
            ));
            parts.add("实习/工作证据：\n- " + String.join("\n- ", workLines));
        } else {
            parts.add("实习/工作证据：当前未提供明确实习或工作经历。");
        }

        parts.add("其他可表扬线索：" + buildCommendableEvidenceSummary(studentProfile, resumeData));
        return String.join("\n", parts);
    }

    private String buildProfileEvidenceSummary(StudentProfileDTO studentProfile) {
        if (studentProfile == null) {
            return "当前没有读取到结构化简历数据，学生画像里也没有明确项目或实习信息。";
        }

        List<String> parts = new ArrayList<>();
        List<ProjectExperienceDTO> projects = projectExperiences(studentProfile);
        List<InternshipExperienceDTO> internships = internshipExperiences(studentProfile);

        if (!projects.isEmpty()) {
            List<String> projectLines = new ArrayList<>();
            projects.forEach(project -> projectLines.add(
                    safeText(project.name(), "未命名项目")
                            + " | 角色=" + safeText(project.role(), "未写角色")
                            + " | 技术=" + joinList(project.techStacks())
                            + " | 描述=" + safeText(project.description(), "未写描述")
                            + " | 亮点=" + safeText(project.highlight(), "未写亮点")
            ));
            parts.add("项目证据：\n- " + String.join("\n- ", projectLines));
        } else {
            parts.add("项目证据：当前未提供明确项目列表。");
        }

        if (!internships.isEmpty()) {
            List<String> workLines = new ArrayList<>();
            internships.forEach(work -> workLines.add(
                    safeText(work.company(), "未写单位")
                            + " | 岗位=" + safeText(work.position(), "未写岗位")
                            + " | 成果=" + safeText(work.achievement(), "未写成果")
            ));
            parts.add("实习/工作证据：\n- " + String.join("\n- ", workLines));
        } else {
            parts.add("实习/工作证据：当前未提供明确实习或工作经历。");
        }

        parts.add("其他可表扬线索：" + buildCommendableEvidenceSummary(studentProfile, null));
        return String.join("\n", parts);
    }

    private String buildCommendableEvidenceSummary(StudentProfileDTO studentProfile, ResumeDTO resumeData) {
        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        if (resumeData != null && resumeData.projects() != null && !resumeData.projects().isEmpty()) {
            highlights.add("已有项目经历");
        }
        if (resumeData != null && resumeData.workExperience() != null && !resumeData.workExperience().isEmpty()) {
            highlights.add("已有实习或工作经历");
        }
        if (resumeData == null && !projectExperiences(studentProfile).isEmpty()) {
            highlights.add("已有项目经历");
        }
        if (resumeData == null && !internshipExperiences(studentProfile).isEmpty()) {
            highlights.add("已有实习或工作经历");
        }
        if (resumeData != null && resumeData.score() != null && resumeData.score().advantages() != null && !resumeData.score().advantages().isEmpty()) {
            highlights.addAll(resumeData.score().advantages().stream().limit(5).toList());
        }

        String combined = collectResumeText(studentProfile, resumeData);
        if (containsAny(combined, "科研", "论文", "实验", "课题", "研究")) {
            highlights.add("存在科研或课题相关线索");
        }
        if (containsAny(combined, "竞赛", "比赛", "挑战杯", "大赛", "获奖")) {
            highlights.add("存在竞赛或获奖相关线索");
        }
        if (containsAny(combined, "志愿", "社会实践", "社团", "学生会", "组织")) {
            highlights.add("存在社会实践或组织协作相关线索");
        }

        return highlights.isEmpty()
                ? "当前可表扬的证据较少，需要模型谨慎判断"
                : String.join("、", highlights);
    }

    private String collectResumeText(StudentProfileDTO studentProfile, ResumeDTO resumeData) {
        StringBuilder builder = new StringBuilder();
        if (resumeData != null && resumeData.projects() != null) {
            resumeData.projects().forEach(project -> builder
                    .append(' ')
                    .append(safeText(project.name(), ""))
                    .append(' ')
                    .append(safeText(project.description(), ""))
                    .append(' ')
                    .append(safeText(project.highlight(), "")));
        }
        if (resumeData != null && resumeData.workExperience() != null) {
            resumeData.workExperience().forEach(work -> builder
                    .append(' ')
                    .append(safeText(work.position(), ""))
                    .append(' ')
                    .append(safeText(work.achievement(), "")));
        }
        if (resumeData == null) {
            projectExperiences(studentProfile).forEach(project -> builder
                    .append(' ')
                    .append(safeText(project.name(), ""))
                    .append(' ')
                    .append(safeText(project.description(), ""))
                    .append(' ')
                    .append(safeText(project.highlight(), "")));
            internshipExperiences(studentProfile).forEach(work -> builder
                    .append(' ')
                    .append(safeText(work.position(), ""))
                    .append(' ')
                    .append(safeText(work.achievement(), "")));
        }
        if (resumeData != null && resumeData.score() != null && resumeData.score().advantages() != null) {
            resumeData.score().advantages().forEach(item -> builder.append(' ').append(item));
        }
        return builder.toString();
    }

    private String buildDoNotWasteTimeContext(StudentProfileDTO studentProfile,
                                              JobProfileDTO targetJob,
                                              ResumeDTO resumeData) {
        List<String> warnings = new ArrayList<>();
        int projectCount = resumeData == null || resumeData.projects() == null ? projectExperiences(studentProfile).size() : resumeData.projects().size();
        int workCount = resumeData == null || resumeData.workExperience() == null ? internshipExperiences(studentProfile).size() : resumeData.workExperience().size();

        if (projectCount >= 2) {
            warnings.add("学生已经有多个项目，短期内不应再为了凑数量重复做同质化项目，更应该整理现有项目的背景、职责、难点、结果和个人贡献。");
        }
        if (workCount > 0) {
            warnings.add("学生已经有实习或工作经历，不应把时间再浪费在补一段形式化经历上，而应优先提炼已有经历的成果与说服力。");
        }
        if (studentProfile != null && studentProfile.certificates() != null && !studentProfile.certificates().isEmpty()) {
            warnings.add("学生已经有证书基础，当前不应盲目继续考一堆边际价值不高的证书，除非目标岗位明确需要。");
        }
        if (studentProfile != null && studentProfile.skills() != null && studentProfile.skills().size() >= 6) {
            warnings.add("学生已经有一定技术基础，短期内不应贪多求全地广撒网学新技术，更适合先把现有技能和已有项目打磨扎实。");
        }
        if (containsAny(targetJob.title(), "项目经理", "产品专员", "产品经理")) {
            warnings.add("当前阶段不应急着用管理者视角包装自己，重点仍然是用事实证明自己具备扎实执行、推进和理解业务的能力。");
        }
        if (warnings.isEmpty()) {
            warnings.add("不要把时间花在看起来很努力、但对当前阶段帮助有限的事情上。先做最贴近当前阶段要求的动作。");
        }
        return String.join("\n", warnings);
    }

    private String buildTargetJobSummary(JobProfileDTO targetJob) {
        return """
                目标岗位族：%s
                岗位定位：%s
                代表薪资：%s
                核心技能：%s
                """.formatted(
                safeText(targetJob.title(), "未提供"),
                trimTo(safeText(targetJob.description(), "未提供"), 180),
                safeText(targetJob.salaryRange(), "未提供"),
                trimTo(joinList(targetJob.requiredSkills()), 120)
        );
    }

    private String buildCurrentStageSummary(JobProfileDTO targetJob, StudentProfileDTO studentProfile) {
        Map<String, Object> stage = careerFamilyPlannerService.summarizeCurrentStage(targetJob, studentProfile);
        return """
                当前更适合进入：%s
                所处阶段：%s
                判断依据：%s
                """.formatted(
                stringOf(stage.get("jobTitle"), "未提供"),
                stringOf(stage.get("phaseName"), "未提供"),
                trimTo(stringOf(stage.get("reason"), "未提供"), 180)
        );
    }

    private String buildMatchSummary(MatchResultDTO matchResult) {
        return """
                综合匹配度：%s%%
                基础条件匹配：%s%%
                核心技能匹配：%s%%
                """.formatted(
                matchResult.matchScore(),
                matchResult.dimensionScores().basicRequirements(),
                matchResult.dimensionScores().professionalSkills()
        );
    }

    private String buildSkillGapSummary(SkillGapAnalysisDTO skillGap) {
        return """
                已匹配能力：%s
                主要缺口：%s
                总结：%s
                """.formatted(
                trimTo(joinList(skillGap.matchedSkills()), 100),
                trimTo(joinList(skillGap.missingSkills()), 120),
                trimTo(safeText(skillGap.summary(), "未提供"), 180)
        );
    }

    private String buildCareerPathSummary(CareerPathResponseDTO careerPath) {
        List<String> vertical = new ArrayList<>();
        careerPath.verticalPath().stream().limit(4).forEach(item ->
                vertical.add(stringOf(item.get("phaseName"), "阶段") + "：" + stringOf(item.get("jobTitle"), "未标注")));
        List<String> horizontal = new ArrayList<>();
        if (!careerPath.horizontalPath().isEmpty()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> paths = (List<Map<String, Object>>) careerPath.horizontalPath().get(0).getOrDefault("paths", List.of());
            paths.stream().limit(3).forEach(item ->
                    horizontal.add(stringOf(item.get("jobTitle"), "未标注") + "：" + trimTo(stringOf(item.get("switchReason"), "未提供"), 80)));
        }
        return """
                纵向发展：%s
                可延展方向：%s
                """.formatted(
                vertical.isEmpty() ? "未提供" : String.join(" -> ", vertical),
                horizontal.isEmpty() ? "未提供" : String.join("；", horizontal)
        );
    }

    private String buildGrowthPlanSummary(GrowthPlanResponseDTO growthPlan) {
        return """
                短期重点：%s
                中期重点：%s
                """.formatted(
                trimTo(String.join("；", summarizeGrowthItems(growthPlan.shortTermPlan())), 180),
                trimTo(String.join("；", summarizeGrowthItems(growthPlan.midTermPlan())), 180)
        );
    }

    private String buildCompactResumeEvidence(StudentProfileDTO studentProfile, ResumeDTO resumeData) {
        if (resumeData == null) {
            List<String> items = new ArrayList<>();
            projectExperiences(studentProfile).stream().limit(3).forEach(project ->
                    items.add("项目：" + safeText(project.name(), "未命名")
                            + "；角色=" + safeText(project.role(), "未写")
                            + "；技术=" + trimTo(joinList(project.techStacks()), 50)
                            + "；亮点=" + trimTo(safeText(project.highlight(), "未写"), 60)));
            internshipExperiences(studentProfile).stream().limit(2).forEach(work ->
                    items.add("实习/工作：" + safeText(work.company(), "未写单位")
                            + "；岗位=" + safeText(work.position(), "未写岗位")
                            + "；成果=" + trimTo(safeText(work.achievement(), "未写"), 60)));
            return items.isEmpty() ? "当前没有结构化简历数据，也没有明确项目或实习条目。" : String.join("\n", items);
        }
        List<String> items = new ArrayList<>();
        if (resumeData.projects() != null) {
            resumeData.projects().stream().limit(3).forEach(project ->
                    items.add("项目：" + safeText(project.name(), "未命名")
                            + "；角色=" + safeText(project.role(), "未写")
                            + "；技术=" + trimTo(joinList(project.techStacks()), 50)
                            + "；亮点=" + trimTo(safeText(project.highlight(), "未写"), 60)));
        }
        if (resumeData.workExperience() != null) {
            resumeData.workExperience().stream().limit(2).forEach(work ->
                    items.add("实习/工作：" + safeText(work.company(), "未写单位")
                            + "；岗位=" + safeText(work.position(), "未写岗位")
                            + "；成果=" + trimTo(safeText(work.achievement(), "未写"), 60)));
        }
        return items.isEmpty() ? "当前没有明确项目或实习条目。" : String.join("\n", items);
    }

    private String buildSoftSkillSummary(AbilityDescriptionsDTO descriptions) {
        if (descriptions == null) {
            return "未提供";
        }
        if (!blank(descriptions.softSkill())) {
            return descriptions.softSkill();
        }
        return "学习=" + safeText(descriptions.learning(), "未提供")
                + "；沟通=" + safeText(descriptions.communication(), "未提供")
                + "；抗压=" + safeText(descriptions.stressTolerance(), "未提供")
                + "；实践=" + safeText(descriptions.internship(), "未提供")
                + "；进取心/创新=" + safeText(descriptions.innovation(), "未提供");
    }

    private List<ProjectExperienceDTO> projectExperiences(StudentProfileDTO studentProfile) {
        return studentProfile == null || studentProfile.projectExperiences() == null
                ? List.of()
                : studentProfile.projectExperiences();
    }

    private List<InternshipExperienceDTO> internshipExperiences(StudentProfileDTO studentProfile) {
        return studentProfile == null || studentProfile.internshipExperiences() == null
                ? List.of()
                : studentProfile.internshipExperiences();
    }

    private List<String> summarizeGrowthItems(GrowthPlanDTO growthPlan) {
        if (growthPlan == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (growthPlan.learningPath() != null) {
            growthPlan.learningPath().stream().limit(3).forEach(item ->
                    result.add(stringOf(item.get("title"), "学习任务") + "："
                            + trimTo(stringOf(item.get("detail"), "围绕当前阶段能力差距推进"), 70)));
        }
        if (growthPlan.practiceArrangements() != null) {
            growthPlan.practiceArrangements().stream().limit(3).forEach(item ->
                    result.add(stringOf(item.get("title"), "实践任务") + "："
                            + trimTo(stringOf(item.get("detail"), "形成可验证的项目或实践成果"), 70)));
        }
        return result;
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String source = text.toLowerCase();
        for (String keyword : keywords) {
            if (source.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String trimTo(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private void checkField(Object value, String fieldName, List<String> completedItems, List<String> missingItems) {
        if (value == null) {
            missingItems.add(fieldName);
            return;
        }
        if (value instanceof String stringValue && stringValue.isBlank()) {
            missingItems.add(fieldName);
            return;
        }
        completedItems.add(fieldName);
    }

    private String stringifyMarkdown(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String extractStudentName(Map<String, Object> reportContent) {
        Object profile = reportContent.get("student_profile");
        if (profile instanceof Map<?, ?> profileMap) {
            Object basicInfo = profileMap.get("basic_info");
            if (basicInfo instanceof Map<?, ?> basicInfoMap) {
                Object name = basicInfoMap.get("name");
                if (name != null && !String.valueOf(name).isBlank()) {
                    return String.valueOf(name).trim();
                }
            }
        }
        return "student";
    }

    private String buildHtmlReport(String markdown, ExportRequestDTO request) {
        String escaped = escapeHtml(markdown).replace("\n", "<br/>");
        String header = blank(request.headerText()) ? "Career Agent Plus" : request.headerText();
        String footer = blank(request.footerText()) ? "" : request.footerText();
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Career Report</title>
                  <style>
                    body { font-family: "Microsoft YaHei", sans-serif; margin: 40px auto; max-width: 920px; line-height: 1.75; color: #1f2937; }
                    header { margin-bottom: 24px; font-size: 14px; color: #6b7280; }
                    article { padding: 24px 28px; border: 1px solid #e5e7eb; border-radius: 16px; background: #ffffff; }
                    footer { margin-top: 18px; font-size: 12px; color: #9ca3af; }
                  </style>
                </head>
                <body>
                  <header>%s</header>
                  <article>%s</article>
                  <footer>%s</footer>
                </body>
                </html>
                """.formatted(escapeHtml(header), escaped, escapeHtml(footer));
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String resolveContentType(String format) {
        if ("html".equalsIgnoreCase(format)) {
            return "text/html;charset=UTF-8";
        }
        return "text/markdown;charset=UTF-8";
    }

    private String safeStudentId(StudentProfileDTO studentProfile) {
        return studentProfile == null || blank(studentProfile.studentId())
                ? "anonymous"
                : studentProfile.studentId();
    }

    private String joinList(List<String> values) {
        return values == null || values.isEmpty() ? "当前证据不足，暂不下结论" : String.join("、", values);
    }

    private String safeCount(List<?> values) {
        return values == null ? "0" : String.valueOf(values.size());
    }

    private String safeNumber(Integer value) {
        return value == null ? "未提供" : String.valueOf(value);
    }

    private String stringOf(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private String safeText(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    private String stripCodeFence(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
