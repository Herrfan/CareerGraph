package com.zust.qyf.careeragent.web;

import com.zust.qyf.careeragent.application.CareerAssistantWorkflowService;
import com.zust.qyf.careeragent.application.AuthService;
import com.zust.qyf.careeragent.application.ChatHistoryService;
import com.zust.qyf.careeragent.domain.dto.knowledge.KnowledgeSnippetDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final com.zust.qyf.careeragent.ai.llm.AIService aiService;
    private final AuthService authService;
    private final ChatHistoryService chatHistoryService;
    private final CareerAssistantWorkflowService careerAssistantWorkflowService;

    public AIController(com.zust.qyf.careeragent.ai.llm.AIService aiService,
                        AuthService authService,
                        ChatHistoryService chatHistoryService,
                        CareerAssistantWorkflowService careerAssistantWorkflowService) {
        this.aiService = aiService;
        this.authService = authService;
        this.chatHistoryService = chatHistoryService;
        this.careerAssistantWorkflowService = careerAssistantWorkflowService;
    }

    @GetMapping(value = "/askAI/{que}", produces = "text/plain;charset=UTF-8")
    public Flux<String> askAI(@PathVariable String que) {
        return aiService.memoryChat(que);
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody ChatRequest request) {
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.sessionId();

        String reply;
        var currentUser = authService.resolveUser(authorization);
        if (currentUser.isPresent()) {
            String conversationKey = "user-" + currentUser.get().id();
            String history = chatHistoryService.buildRecentHistory(currentUser.get().id(), conversationKey);
            reply = careerAssistantWorkflowService.answerWithHistory(history, request.message(), request.context());
            chatHistoryService.saveUserMessage(currentUser.get().id(), conversationKey, request.message());
            chatHistoryService.saveAssistantMessage(currentUser.get().id(), conversationKey, reply == null ? "" : reply);
            sessionId = conversationKey;
        } else {
            reply = careerAssistantWorkflowService.answerStateless(request.message(), request.context());
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("session_id", sessionId);
        result.put("reply", reply == null ? "" : reply);
        return result;
    }

    @GetMapping("/knowledge-search")
    public Map<String, Object> knowledgeSearch(@RequestParam("query") String query,
                                               @RequestParam(name = "target_job", required = false) String targetJob) {
        java.util.List<KnowledgeSnippetDTO> snippets = careerAssistantWorkflowService.searchKnowledge(query, targetJob);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("target_job", targetJob == null ? "" : targetJob);
        result.put("snippets", snippets);
        return result;
    }

    public record ChatRequest(String sessionId, String message, String context) {}
}
