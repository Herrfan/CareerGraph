package com.zust.qyf.careeragent.ai.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.application.ResumeService;
import com.zust.qyf.careeragent.domain.dto.ResumeDTO;
import com.zust.qyf.careeragent.domain.dto.student.AbilityDescriptionsDTO;
import com.zust.qyf.careeragent.utils.PromptUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AIService {
    private final ChatClient pureChatClient;
    private final ChatClient memoryChatClient;
    private final PromptUtil promptUtil;
    private final ResumeService resumeService;
    private final ObjectMapper objectMapper;

    public AIService(@Qualifier("pureChatClient") ChatClient pureChatClient,
                     @Qualifier("memoryChatClient") ChatClient memoryChatClient,
                     PromptUtil promptUtil,
                     ResumeService resumeService,
                     ObjectMapper objectMapper) {
        this.pureChatClient = pureChatClient;
        this.memoryChatClient = memoryChatClient;
        this.promptUtil = promptUtil;
        this.resumeService = resumeService;
        this.objectMapper = objectMapper;
    }

    public Flux<String> pureChat(String query) {
        return pureChatClient.prompt()
                .user(query)
                .stream()
                .content();
    }

    public Flux<String> memoryChat(String query) {
        return memoryChatClient.prompt()
                .user(query)
                .stream()
                .content();
    }

    public ResumeDTO analyseResume(String cleanText) {
        String systemPrompt = promptUtil.getPrompt("resume_portrait.txt");
        String prompt = """
                请基于以下简历原文完成学生画像结构化抽取。
                只输出 JSON，不要输出任何解释、注释或 Markdown。

                [简历原文]
                %s
                """.formatted(cleanText == null ? "" : cleanText);

        ResumeDTO result = pureChatClient.prompt()
                .system(systemPrompt)
                .user(prompt)
                .options(ChatOptions.builder().temperature(0.1).maxTokens(6000).build())
                .call()
                .entity(ResumeDTO.class);

        if (result == null) {
            throw new IllegalStateException("Kimi did not return a structured resume result");
        }

        resumeService.saveResumeData("1", result);
        return result;
    }

    public AbilityDescriptionsDTO generateAbilityDescriptions(String cleanText, ResumeDTO resumeDTO) {
        String systemPrompt = promptUtil.getPrompt("student_profile_ability_descriptions.txt");
        String resumeJson = toJson(resumeDTO);
        String prompt = """
                请基于以下简历原文与结构化解析结果，生成学生画像中的能力描述。
                只输出 JSON，不要输出任何解释、注释或 Markdown。

                [简历原文]
                %s

                [结构化简历 JSON]
                %s
                """.formatted(cleanText == null ? "" : cleanText, resumeJson);

        AbilityDescriptionsDTO result = pureChatClient.prompt()
                .system(systemPrompt)
                .user(prompt)
                .options(ChatOptions.builder().temperature(0.35).maxTokens(1200).build())
                .call()
                .entity(AbilityDescriptionsDTO.class);

        if (result == null) {
            throw new IllegalStateException("Kimi did not return student profile ability descriptions");
        }
        return result;
    }

    public String chatWithMemory(String sessionId, String userMessage) {
        return memoryChatClient.prompt()
                .user(userMessage)
                .advisors(advisors -> advisors.param("chat_memory_conversation_id", sessionId))
                .call()
                .content();
    }

    public String chatWithoutMemory(String userMessage) {
        return pureChatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }

    public String chatWithoutMemoryWithGrounding(String userMessage, String groundingContext) {
        return groundedChat("", userMessage, groundingContext);
    }

    public String chatWithGroundedHistory(String history, String userMessage, String groundingContext) {
        return groundedChat(history, userMessage, groundingContext);
    }

    public String chatWithPersistentHistory(String history, String userMessage) {
        String prompt = """
                [Conversation History]
                %s

                [Current User Message]
                %s
                """.formatted(history == null ? "" : history, userMessage == null ? "" : userMessage);

        return pureChatClient.prompt()
                .system("你是一名务实、直接、可靠的大学生职业规划顾问。请基于已有上下文给出具体建议，不要空话。")
                .user(prompt)
                .options(ChatOptions.builder().temperature(0.4).build())
                .call()
                .content();
    }

    private String groundedChat(String history, String userMessage, String groundingContext) {
        String prompt = """
                [Grounding Context]
                %s

                [Conversation History]
                %s

                [Current User Message]
                %s
                """.formatted(
                groundingContext == null ? "" : groundingContext,
                history == null ? "" : history,
                userMessage == null ? "" : userMessage
        );

        return pureChatClient.prompt()
                .system("""
                        你是一名面向大学生职业规划场景的 AI 助手。
                        请优先使用提供的图谱洞察和本地知识库片段作答。
                        不允许编造岗位要求、证书、薪资、城市层级或职业路径事实。
                        如果证据不足，要明确说“不确定”或“当前知识不足”。
                        回答优先给出：结论、依据、下一步建议。
                        """)
                .user(prompt)
                .options(ChatOptions.builder().temperature(0.35).build())
                .call()
                .content();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize prompt context", e);
        }
    }
}
