package com.zust.qyf.careeragent.web;

import com.zust.qyf.careeragent.application.AuthService;
import com.zust.qyf.careeragent.application.ReportApplicationService;
import com.zust.qyf.careeragent.domain.dto.report.CheckCompletenessRequestDTO;
import com.zust.qyf.careeragent.domain.dto.report.CompletenessCheckResponseDTO;
import com.zust.qyf.careeragent.domain.dto.report.ExportRequestDTO;
import com.zust.qyf.careeragent.domain.dto.report.GeneratedReportDTO;
import com.zust.qyf.careeragent.domain.dto.report.GrowthPlanRequestDTO;
import com.zust.qyf.careeragent.domain.dto.report.GrowthPlanResponseDTO;
import com.zust.qyf.careeragent.domain.dto.report.PolishReportRequestDTO;
import com.zust.qyf.careeragent.domain.dto.report.ReportExportResponseDTO;
import com.zust.qyf.careeragent.domain.dto.report.ReportGenerateRequestDTO;
import com.zust.qyf.careeragent.domain.dto.report.ReportSnapshotDTO;
import com.zust.qyf.careeragent.domain.dto.report.SaveReportSnapshotRequestDTO;
import com.zust.qyf.careeragent.domain.dto.report.ProfileScoringDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {
    private final AuthService authService;
    private final ReportApplicationService reportApplicationService;

    public ReportController(AuthService authService,
                            ReportApplicationService reportApplicationService) {
        this.authService = authService;
        this.reportApplicationService = reportApplicationService;
    }

    @PostMapping("/generate")
    public GeneratedReportDTO generate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody ReportGenerateRequestDTO request) {
        Long userId = authService.resolveUser(authorization).map(AuthService.AuthenticatedUser::id).orElse(null);
        return reportApplicationService.generateReport(request, userId);
    }

    @PostMapping("/polish")
    public Map<String, Object> polish(@RequestBody PolishReportRequestDTO request) {
        return reportApplicationService.polishReport(request);
    }

    @PostMapping("/check-completeness")
    public CompletenessCheckResponseDTO checkCompleteness(@RequestBody CheckCompletenessRequestDTO request) {
        return reportApplicationService.checkCompleteness(request.reportContent());
    }

    @PostMapping("/growth-plan/generate")
    public GrowthPlanResponseDTO generateGrowthPlan(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @RequestBody GrowthPlanRequestDTO request) {
        Long userId = authService.resolveUser(authorization).map(AuthService.AuthenticatedUser::id).orElse(null);
        return reportApplicationService.generateGrowthPlan(request.studentProfile(), request.targetJob(), userId);
    }

    @PostMapping("/export")
    public ReportExportResponseDTO export(@RequestBody ExportRequestDTO request) {
        return reportApplicationService.exportReport(request);
    }

    @GetMapping("/snapshot/latest")
    public ReportSnapshotDTO latest(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestParam("target_job") String targetJob) {
        AuthService.AuthenticatedUser user = authService.resolveUser(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return reportApplicationService.getLatestSnapshot(user.id(), targetJob)
                .orElseThrow(() -> new IllegalArgumentException("report snapshot not found"));
    }

    @GetMapping("/snapshot/history/{studentId}")
    public List<ReportSnapshotDTO> history(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @PathVariable String studentId) {
        AuthService.AuthenticatedUser user = authService.resolveUser(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return reportApplicationService.listSnapshots(user.id());
    }

    @PostMapping("/snapshot/save")
    public ReportSnapshotDTO saveSnapshot(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @RequestBody SaveReportSnapshotRequestDTO request) {
        AuthService.AuthenticatedUser user = authService.resolveUser(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return reportApplicationService.saveSnapshot(user.id(), request);
    }

    @PostMapping("/score")
    public ProfileScoringDTO scoreProfile(@RequestBody StudentProfileDTO studentProfile,
                                          @RequestParam(value = "target_job", required = false) String targetJob) {
        return reportApplicationService.scoreProfile(studentProfile, targetJob);
    }
}
