package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.graph.GraphRoleContextDTO;
import com.zust.qyf.careeragent.domain.dto.graph.GraphSimilarJobDTO;
import com.zust.qyf.careeragent.domain.dto.graph.SkillGapAnalysisDTO;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import com.zust.qyf.careeragent.infrastructure.graph.EnterpriseGraphQueryService;
import com.zust.qyf.careeragent.infrastructure.knowledge.JobCatalogService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class EnterpriseGraphInsightsApplicationService {
    private final EnterpriseGraphQueryService enterpriseGraphQueryService;
    private final KnowledgeImportService knowledgeImportService;
    private final JobCatalogService jobCatalogService;

    public EnterpriseGraphInsightsApplicationService(EnterpriseGraphQueryService enterpriseGraphQueryService,
                                                     KnowledgeImportService knowledgeImportService,
                                                     JobCatalogService jobCatalogService) {
        this.enterpriseGraphQueryService = enterpriseGraphQueryService;
        this.knowledgeImportService = knowledgeImportService;
        this.jobCatalogService = jobCatalogService;
    }

    public List<GraphSimilarJobDTO> findSimilarJobs(String jobTitle, int limit) {
        List<GraphSimilarJobDTO> portraitGraphJobs = enterpriseGraphQueryService.findSimilarPortraitRoles(jobTitle, limit);
        if (!portraitGraphJobs.isEmpty()) {
            return portraitGraphJobs;
        }

        List<GraphSimilarJobDTO> graphJobs = enterpriseGraphQueryService.findSimilarJobs(jobTitle, limit);
        if (!graphJobs.isEmpty()) {
            return graphJobs;
        }

        JobProfileDTO targetJob = resolveTargetJob(jobTitle);
        if (targetJob == null) {
            return List.of();
        }

        return knowledgeImportService.getAllJobs().stream()
                .filter(job -> !job.jobId().equals(targetJob.jobId()))
                .map(job -> new GraphSimilarJobDTO(
                        job.jobId(),
                        job.title(),
                        job.companyName(),
                        job.city(),
                        job.category(),
                        sharedSkills(targetJob, job),
                        similarity(targetJob, job)
                ))
                .filter(job -> job.similarityScore() > 0.15)
                .sorted((left, right) -> Double.compare(right.similarityScore(), left.similarityScore()))
                .limit(Math.max(limit, 1))
                .toList();
    }

    public SkillGapAnalysisDTO analyzeSkillGap(StudentProfileDTO studentProfile, String targetJob) {
        List<String> requiredSkills = enterpriseGraphQueryService.loadPortraitRequiredSkills(targetJob);
        if (requiredSkills.isEmpty()) {
            requiredSkills = enterpriseGraphQueryService.loadRequiredSkills(targetJob);
        }
        JobProfileDTO targetProfile = resolveTargetJob(targetJob);
        if (requiredSkills.isEmpty() && targetProfile != null) {
            requiredSkills = targetProfile.requiredSkills();
        }

        List<String> studentSkills = studentProfile == null || studentProfile.skills() == null
                ? List.of()
                : studentProfile.skills().stream().filter(skill -> skill != null && !skill.isBlank()).distinct().toList();

        List<String> matchedSkills = requiredSkills.stream()
                .filter(required -> studentSkills.stream().anyMatch(skill -> normalize(skill).contains(normalize(required))
                        || normalize(required).contains(normalize(skill))))
                .distinct()
                .toList();

        List<String> missingSkills = requiredSkills.stream()
                .filter(required -> matchedSkills.stream().noneMatch(skill -> normalize(skill).equals(normalize(required))))
                .distinct()
                .toList();

        double matchScore = requiredSkills.isEmpty()
                ? 0.0
                : Math.round((matchedSkills.size() * 1000.0 / requiredSkills.size())) / 10.0;

        String summary;
        if (requiredSkills.isEmpty()) {
            summary = "当前岗位样本未提取到稳定的技能要求，建议先补充岗位清洗结果。";
        } else if (missingSkills.isEmpty()) {
            summary = "当前画像已覆盖该岗位的大部分核心技能，可把重点放到项目证据和面试表达。";
        } else {
            summary = "当前已匹配 " + matchedSkills.size() + " 项核心技能，仍缺少 " + missingSkills.size() + " 项关键技能："
                    + String.join("、", missingSkills) + "。";
        }
        return new SkillGapAnalysisDTO(targetJob, matchedSkills, missingSkills, matchScore, summary);
    }

    public GraphRoleContextDTO getRoleContext(String jobTitle) {
        return enterpriseGraphQueryService.loadPortraitRoleContext(jobTitle)
                .orElseGet(() -> fallbackRoleContext(jobTitle));
    }

    public String buildCareerChatContext(StudentProfileDTO studentProfile, String targetJob) {
        if (targetJob == null || targetJob.isBlank()) {
            return "";
        }
        SkillGapAnalysisDTO skillGap = analyzeSkillGap(studentProfile, targetJob);
        GraphRoleContextDTO roleContext = getRoleContext(targetJob);
        List<GraphSimilarJobDTO> similarJobs = findSimilarJobs(targetJob, 3);
        StringBuilder builder = new StringBuilder();
        builder.append("[Graph Insights]\n");
        builder.append("Target Job: ").append(targetJob).append('\n');
        if (roleContext != null) {
            builder.append("City Tier: ").append(roleContext.cityTier()).append('\n');
            builder.append("Salary Band: ").append(roleContext.salaryBand()).append('\n');
            builder.append("Role Skills: ").append(String.join(", ", roleContext.requiredSkills())).append('\n');
            builder.append("Role Abilities: ").append(String.join(", ", roleContext.topAbilities())).append('\n');
            if (!roleContext.relatedRoles().isEmpty()) {
                builder.append("Related Generic Roles: ").append(String.join(", ", roleContext.relatedRoles())).append('\n');
            }
        }
        builder.append("Skill Match: ").append(skillGap.skillMatchScore()).append("%\n");
        builder.append("Matched Skills: ").append(String.join(", ", skillGap.matchedSkills())).append('\n');
        builder.append("Missing Skills: ").append(String.join(", ", skillGap.missingSkills())).append('\n');
        if (!similarJobs.isEmpty()) {
            builder.append("Related Jobs: ").append(
                    similarJobs.stream()
                            .map(job -> job.jobTitle() + "(" + job.similarityScore() + ")")
                            .reduce((left, right) -> left + ", " + right)
                            .orElse("")
            );
        }
        return builder.toString();
    }

    private JobProfileDTO resolveTargetJob(String targetJob) {
        if (targetJob == null || targetJob.isBlank()) {
            return null;
        }
        JobProfileDTO portraitJob = jobCatalogService.searchSimilarJobs(targetJob, 1).stream().findFirst().orElse(null);
        if (portraitJob != null) {
            return portraitJob;
        }
        return knowledgeImportService.searchSimilarJobs(targetJob, 1).stream().findFirst().orElse(null);
    }

    private List<String> sharedSkills(JobProfileDTO left, JobProfileDTO right) {
        return left.requiredSkills().stream()
                .filter(skill -> right.requiredSkills().stream()
                        .anyMatch(candidate -> normalize(candidate).contains(normalize(skill))
                                || normalize(skill).contains(normalize(candidate))))
                .distinct()
                .toList();
    }

    private double similarity(JobProfileDTO left, JobProfileDTO right) {
        double score = 0.0;
        if (normalize(left.category()).equals(normalize(right.category()))) {
            score += 0.35;
        }
        if (normalize(left.title()).contains(normalize(right.title())) || normalize(right.title()).contains(normalize(left.title()))) {
            score += 0.25;
        }
        List<String> sharedSkills = sharedSkills(left, right);
        int base = Math.max(1, Math.max(left.requiredSkills().size(), right.requiredSkills().size()));
        score += sharedSkills.size() * 0.4 / base;
        return Math.round(Math.min(0.98, score) * 100.0) / 100.0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "").replace("/", "").replace("+", "");
    }

    private GraphRoleContextDTO fallbackRoleContext(String jobTitle) {
        JobProfileDTO job = resolveTargetJob(jobTitle);
        if (job == null) {
            return new GraphRoleContextDTO(jobTitle, "", "", List.of(), List.of(), List.of(), List.of(), "");
        }

        List<String> topAbilities = job.abilityPriority() == null
                ? List.of()
                : job.abilityPriority().entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(5)
                .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                .toList();

        List<String> relatedRoles = knowledgeImportService.searchSimilarJobs(job.title(), 4).stream()
                .filter(candidate -> !candidate.jobId().equals(job.jobId()))
                .limit(3)
                .map(JobProfileDTO::title)
                .toList();

        return new GraphRoleContextDTO(
                job.title(),
                safe(job.cityTier()),
                safe(job.salaryBand()),
                job.requiredSkills() == null ? List.of() : job.requiredSkills(),
                job.requiredCertificates() == null ? List.of() : job.requiredCertificates(),
                topAbilities,
                relatedRoles,
                "Fallback role context from processed portrait knowledge."
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
