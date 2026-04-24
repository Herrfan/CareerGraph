package com.zust.qyf.careeragent.web;

import com.zust.qyf.careeragent.application.AuthService;
import com.zust.qyf.careeragent.application.ChatHistoryService;
import com.zust.qyf.careeragent.application.ReportApplicationService;
import com.zust.qyf.careeragent.application.StudentProfilePersistenceService;
import com.zust.qyf.careeragent.domain.dto.chat.ChatMessageDTO;
import com.zust.qyf.careeragent.domain.dto.report.ReportSnapshotDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileSnapshotDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final AuthService authService;
    private final StudentProfilePersistenceService studentProfilePersistenceService;
    private final ReportApplicationService reportApplicationService;
    private final ChatHistoryService chatHistoryService;

    public HistoryController(AuthService authService,
                             StudentProfilePersistenceService studentProfilePersistenceService,
                             ReportApplicationService reportApplicationService,
                             ChatHistoryService chatHistoryService) {
        this.authService = authService;
        this.studentProfilePersistenceService = studentProfilePersistenceService;
        this.reportApplicationService = reportApplicationService;
        this.chatHistoryService = chatHistoryService;
    }

    @GetMapping
    public Map<String, Object> history(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthService.AuthenticatedUser user = authService.resolveUser(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));

        List<StudentProfileSnapshotDTO> profiles = studentProfilePersistenceService.listHistoryByUserId(user.id());
        List<ReportSnapshotDTO> reports = reportApplicationService.listSnapshots(user.id());
        List<ChatMessageDTO> chats = chatHistoryService.listRecentMessages(user.id());

        return Map.of(
                "profiles", profiles,
                "reports", reports,
                "chats", chats
        );
    }
}
