package com.zust.qyf.careeragent.web;

import com.zust.qyf.careeragent.application.AuthService;
import com.zust.qyf.careeragent.application.ResumeProfileApplicationService;
import com.zust.qyf.careeragent.domain.dto.ResumeDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileEvaluationDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ResumeController {
    private final AuthService authService;
    private final ResumeProfileApplicationService resumeProfileApplicationService;

    public ResumeController(AuthService authService,
                            ResumeProfileApplicationService resumeProfileApplicationService) {
        this.authService = authService;
        this.resumeProfileApplicationService = resumeProfileApplicationService;
    }

    @PostMapping("/analyse")
    public ResumeDTO analyseResume(@RequestParam("file") MultipartFile file) {
        return resumeProfileApplicationService.analyseResume(file);
    }

    @PostMapping("/api/student/profile")
    public StudentProfileDTO createStudentProfile(@RequestParam("resume") MultipartFile resume,
                                                  @RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @RequestParam(name = "expected_position", required = false, defaultValue = "") String expectedPosition,
                                                  @RequestParam(name = "expected_salary", required = false, defaultValue = "") String expectedSalary,
                                                  @RequestParam(name = "expected_city", required = false, defaultValue = "") String expectedCity) {
        StudentProfileDTO profile = resumeProfileApplicationService.createStudentProfile(
                resume,
                expectedPosition,
                expectedSalary,
                expectedCity
        );
        return authService.resolveUser(authorization)
                .map(user -> resumeProfileApplicationService.saveForUser(user.id(), profile))
                .orElse(profile);
    }

    @PostMapping("/api/student/profile/manual")
    public StudentProfileDTO createManualStudentProfile(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @RequestBody StudentProfileDTO profile) {
        StudentProfileDTO normalized = resumeProfileApplicationService.createManualStudentProfile(profile);
        return authService.resolveUser(authorization)
                .map(user -> resumeProfileApplicationService.saveForUser(user.id(), normalized))
                .orElse(normalized);
    }

    @GetMapping("/api/student/profile/{studentId}")
    public StudentProfileDTO getStudentProfile(@PathVariable String studentId) {
        return resumeProfileApplicationService.getStudentProfile(studentId)
                .orElseThrow(() -> new IllegalArgumentException("student profile not found: " + studentId));
    }

    @GetMapping("/api/student/profile/me")
    public StudentProfileDTO getMyStudentProfile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthService.AuthenticatedUser user = authService.resolveUser(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return resumeProfileApplicationService.getCurrentUserProfile(user.id())
                .orElseThrow(() -> new IllegalArgumentException("当前账号下暂无学生画像"));
    }

    @PostMapping("/api/student/profile/evaluate")
    public StudentProfileEvaluationDTO evaluateStudentProfile(@RequestBody StudentProfileDTO profile) {
        return resumeProfileApplicationService.evaluateStudentProfile(profile);
    }
}
