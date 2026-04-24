package com.zust.qyf.careeragent.web;

import com.zust.qyf.careeragent.application.AdminEnterpriseService;
import com.zust.qyf.careeragent.application.AuthService;
import com.zust.qyf.careeragent.domain.dto.admin.AdminJobUpdateRequestDTO;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.NormalizedJobRecordEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/jobs")
public class AdminEnterpriseController {
    private final AuthService authService;
    private final AdminEnterpriseService adminEnterpriseService;

    public AdminEnterpriseController(AuthService authService, AdminEnterpriseService adminEnterpriseService) {
        this.authService = authService;
        this.adminEnterpriseService = adminEnterpriseService;
    }

    @GetMapping
    public List<NormalizedJobRecordEntity> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireAdmin(authorization);
        return adminEnterpriseService.listJobs();
    }

    @PutMapping("/{sourceId}")
    public NormalizedJobRecordEntity update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @PathVariable String sourceId,
                                            @RequestBody AdminJobUpdateRequestDTO request) {
        authService.requireAdmin(authorization);
        return adminEnterpriseService.updateJob(sourceId, request);
    }
}
