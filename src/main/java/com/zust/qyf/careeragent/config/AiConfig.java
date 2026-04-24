package com.zust.qyf.careeragent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.embedding.model:nomic-embed-text}")
    private String ollamaEmbeddingModel;

    @Value("${ollama.chat.model:qwen3:8b}")
    private String ollamaChatModel;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public OllamaChatModel ollamaChatModel() {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(ollamaBaseUrl)
                .build();
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder().model(ollamaChatModel).build())
                .build();
    }

    @Bean
    @Primary
    public ChatClient.Builder chatClientBuilder(ObjectProvider<OpenAiChatModel> openAiChatModelProvider,
                                                OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(resolveChatModel(openAiChatModelProvider, ollamaChatModel));
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(ollamaBaseUrl)
                .build();

        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                .model(ollamaEmbeddingModel)
                .build();

        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();
    }

    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    @Bean
    public InMemoryChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean("pureChatClient")
    public ChatClient pureChatClient(ObjectProvider<OpenAiChatModel> openAiChatModelProvider,
                                     OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(resolveChatModel(openAiChatModelProvider, ollamaChatModel)).build();
    }

    @Bean("memoryChatClient")
    public ChatClient memoryChatClient(ObjectProvider<OpenAiChatModel> openAiChatModelProvider,
                                       OllamaChatModel ollamaChatModel,
                                       InMemoryChatMemoryRepository chatMemoryRepository) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(15)
                .build();

        return ChatClient.builder(resolveChatModel(openAiChatModelProvider, ollamaChatModel))
                .defaultSystem("你是一位拥有10年互联网大厂经验的资深大学生职业规划顾问。请用专业、真诚、接地气的语气回答用户的求职困惑。优先给出结构化、可执行的建议，必要时再展开细节。")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean("openAiChatClientBuilder")
    @ConditionalOnBean(OpenAiChatModel.class)
    public ChatClient.Builder openAiChatClientBuilder(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel);
    }

    @Bean("openAiPureChatClient")
    @ConditionalOnBean(OpenAiChatModel.class)
    public ChatClient openAiPureChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    private ChatModel resolveChatModel(ObjectProvider<OpenAiChatModel> openAiChatModelProvider,
                                       OllamaChatModel ollamaChatModel) {
        OpenAiChatModel openAiChatModel = openAiChatModelProvider.getIfAvailable();
        return openAiChatModel != null ? openAiChatModel : ollamaChatModel;
    }
}
