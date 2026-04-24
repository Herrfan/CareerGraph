package com.zust.qyf.careeragent.domain.service;

import com.zust.qyf.careeragent.application.CareerAiDecisionService;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.match.CategoryMatchDTO;
import com.zust.qyf.careeragent.domain.dto.match.MatchResultDTO;
import com.zust.qyf.careeragent.domain.dto.student.BasicInfoDTO;
import com.zust.qyf.careeragent.domain.dto.student.JobPreferenceDTO;
import com.zust.qyf.careeragent.domain.dto.student.SoftAbilitiesDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchScoringServiceTest {

    private final CareerAiDecisionService careerAiDecisionService = Mockito.mock(CareerAiDecisionService.class);
    private final MatchScoringService service = new MatchScoringService(careerAiDecisionService);

    @Test
    void categoryGroupingUsesTitleAndSkillsWhenRawCategoryIsDirty() {
        StudentProfileDTO studentProfile = buildStudentProfile("实施工程师", List.of("ArcGIS", "MySQL", "SQL"));
        JobProfileDTO implementationJob = buildJob(
                "实施工程师",
                "frontend",
                List.of("ArcGIS", "Oracle", "MySQL"),
                "广州",
                "8K-12K"
        );

        List<CategoryMatchDTO> categories = service.calculateCategoryMatches(studentProfile, List.of(implementationJob));

        assertEquals(1, categories.size());
        assertEquals("实施工程师", categories.get(0).category());
    }

    @Test
    void matchScoreDoesNotCollapseWhenImplementationJobIsMisclassifiedAsFrontend() {
        StudentProfileDTO studentProfile = buildStudentProfile("实施工程师", List.of("ArcGIS", "MySQL", "SQL"));
        JobProfileDTO implementationJob = buildJob(
                "实施工程师",
                "frontend",
                List.of("ArcGIS", "Oracle", "MySQL"),
                "广州",
                "8K-12K"
        );

        MatchResultDTO result = service.calculateMatch(studentProfile, implementationJob);

        assertTrue(result.dimensionScores().basicRequirements() >= 70);
        assertTrue(result.dimensionScores().professionalSkills() >= 60);
        assertTrue(result.matchScore() >= 65);
    }

    private StudentProfileDTO buildStudentProfile(String expectedPosition, List<String> skills) {
        return new StudentProfileDTO(
                "student-1",
                new BasicInfoDTO("测试学生", "本科", "信息管理", "浙江工业大学", "2026"),
                skills,
                List.of("软考"),
                new SoftAbilitiesDTO(70, 78, 72, 75, 74, 68, 80),
                new JobPreferenceDTO(expectedPosition, "8K-12K", "广州"),
                null,
                List.of(),
                List.of()
        );
    }

    private JobProfileDTO buildJob(String title,
                                   String category,
                                   List<String> requiredSkills,
                                   String city,
                                   String salaryRange) {
        return new JobProfileDTO(
                "job-1",
                title,
                "交付中心",
                "负责客户现场实施、系统部署、数据建库和上线支持。",
                "负责客户现场实施、系统部署、数据建库和上线支持。",
                category,
                requiredSkills,
                List.of("无"),
                Map.of("basic_requirements", 0.6, "professional_skills", 0.22, "professional_quality", 0.08, "growth_potential", 0.10),
                Map.of(),
                Map.of(),
                salaryRange,
                city,
                "1-3年",
                "南方数码",
                city,
                "地理信息",
                "500-999人",
                "民营",
                "CC001",
                "公司介绍",
                "generic_role_portrait",
                "二线城市",
                "5K-10K",
                3,
                0.9,
                List.of("source-1", "source-2"),
                List.of()
        );
    }
}
