package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.match.CategoryMatchDTO;
import com.zust.qyf.careeragent.domain.dto.match.DimensionScoreDTO;
import com.zust.qyf.careeragent.domain.dto.match.JobMatchDTO;
import com.zust.qyf.careeragent.domain.dto.match.MatchRequestDTO;
import com.zust.qyf.careeragent.domain.dto.match.MatchResultDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import com.zust.qyf.careeragent.domain.service.MatchScoringService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchApplicationService {
    private final KnowledgeImportService knowledgeImportService;
    private final MatchScoringService matchScoringService;

    public MatchApplicationService(KnowledgeImportService knowledgeImportService, MatchScoringService matchScoringService) {
        this.knowledgeImportService = knowledgeImportService;
        this.matchScoringService = matchScoringService;
    }

    public MatchResultDTO calculateMatch(MatchRequestDTO request) {
        validateStudentProfile(request.studentProfile());
        if (request.jobId() == null || request.jobId().isBlank()) {
            throw new IllegalArgumentException("jobId 不能为空");
        }
        JobProfileDTO job = knowledgeImportService.getJob(request.jobId())
                .orElseThrow(() -> new IllegalArgumentException("未找到对应岗位: " + request.jobId()));
        return matchScoringService.calculateMatch(request.studentProfile(), job);
    }

    public List<JobMatchDTO> batchMatch(MatchRequestDTO request) {
        validateStudentProfile(request.studentProfile());
        int topN = request.topN() == null ? 10 : request.topN();
        List<JobProfileDTO> jobs = filterJobsByCategory(request.category());
        return matchScoringService.calculateTopMatches(request.studentProfile(), jobs, topN);
    }

    public List<CategoryMatchDTO> categoryMatches(StudentProfileDTO studentProfile) {
        validateStudentProfile(studentProfile);
        return matchScoringService.calculateCategoryMatches(studentProfile, knowledgeImportService.getAllJobs());
    }

    public List<JobMatchDTO> getCategoryJobs(String category, Integer limit) {
        int safeLimit = limit == null ? 5 : Math.max(limit, 1);
        DimensionScoreDTO emptyScores = new DimensionScoreDTO(0, 0, 0, 0);
        return filterJobsByCategory(category).stream()
                .limit(safeLimit)
                .map(job -> new JobMatchDTO(job, 0, emptyScores))
                .toList();
    }

    private List<JobProfileDTO> filterJobsByCategory(String category) {
        List<JobProfileDTO> jobs = knowledgeImportService.getAllJobs();
        if (category == null || category.isBlank()) {
            return jobs;
        }
        String normalizedCategory = normalizeText(category);
        return jobs.stream()
                .filter(job -> {
                    String jobCategory = normalizeText(job.category());
                    String jobTitle = normalizeText(job.title());
                    return normalizedCategory.equals(jobCategory)
                            || normalizedCategory.equals(jobTitle)
                            || jobTitle.contains(normalizedCategory)
                            || jobCategory.contains(normalizedCategory);
                })
                .toList();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("\\s+", "")
                .replace("/", "")
                .replace("（", "(")
                .replace("）", ")");
    }

    private void validateStudentProfile(StudentProfileDTO studentProfile) {
        if (studentProfile == null) {
            throw new IllegalArgumentException("studentProfile 不能为空");
        }
        if (studentProfile.basicInfo() == null) {
            throw new IllegalArgumentException("studentProfile.basicInfo 不能为空");
        }
    }
}
