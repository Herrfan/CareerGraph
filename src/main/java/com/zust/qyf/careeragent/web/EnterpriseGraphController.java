package com.zust.qyf.careeragent.web;

import com.zust.qyf.careeragent.application.EnterpriseGraphBootstrapApplicationService;
import com.zust.qyf.careeragent.application.EnterpriseGraphInsightsApplicationService;
import com.zust.qyf.careeragent.domain.dto.graph.GraphRoleContextDTO;
import com.zust.qyf.careeragent.domain.dto.graph.GraphSimilarJobDTO;
import com.zust.qyf.careeragent.domain.dto.graph.SkillGapAnalysisDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class EnterpriseGraphController {
    private final EnterpriseGraphInsightsApplicationService enterpriseGraphInsightsApplicationService;
    private final EnterpriseGraphBootstrapApplicationService enterpriseGraphBootstrapApplicationService;

    public EnterpriseGraphController(EnterpriseGraphInsightsApplicationService enterpriseGraphInsightsApplicationService,
                                     EnterpriseGraphBootstrapApplicationService enterpriseGraphBootstrapApplicationService) {
        this.enterpriseGraphInsightsApplicationService = enterpriseGraphInsightsApplicationService;
        this.enterpriseGraphBootstrapApplicationService = enterpriseGraphBootstrapApplicationService;
    }

    @PostMapping("/bootstrap")
    public Map<String, Object> bootstrap(@RequestParam(name = "force_sync", defaultValue = "true") boolean forceSync) {
        return enterpriseGraphBootstrapApplicationService.bootstrapFromMysql(forceSync);
    }

    @PostMapping("/bootstrap-portraits")
    public Map<String, Object> bootstrapPortraits(@RequestParam(name = "force_sync", defaultValue = "true") boolean forceSync) {
        return enterpriseGraphBootstrapApplicationService.bootstrapFromPortraits(forceSync);
    }

    @GetMapping("/similar-jobs")
    public Map<String, Object> similarJobs(@RequestParam("job_title") String jobTitle,
                                           @RequestParam(defaultValue = "5") int limit) {
        List<GraphSimilarJobDTO> jobs = enterpriseGraphInsightsApplicationService.findSimilarJobs(jobTitle, limit);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("job_title", jobTitle);
        result.put("jobs", jobs);
        return result;
    }

    @GetMapping("/role-context")
    public GraphRoleContextDTO roleContext(@RequestParam("job_title") String jobTitle) {
        return enterpriseGraphInsightsApplicationService.getRoleContext(jobTitle);
    }

    @PostMapping("/skill-gap")
    public SkillGapAnalysisDTO skillGap(@RequestBody SkillGapRequest request) {
        return enterpriseGraphInsightsApplicationService.analyzeSkillGap(request.studentProfile(), request.targetJob());
    }

    public record SkillGapRequest(StudentProfileDTO studentProfile, String targetJob) {}
}
