package com.zust.qyf.careeragent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaExcelJobDatasetManual {

    @Test
    void cleanWorkspaceExcelWithOllamaQwen3() throws Exception {
        Path excelPath;
        try (var stream = Files.list(Path.of("."))) {
            excelPath = stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".xls") || name.endsWith(".xlsx");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No Excel file found in workspace root"));
        }

        String baseUrl = System.getProperty("ollama.baseUrl",
                System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"));
        String model = System.getProperty("ollama.chatModel",
                System.getenv().getOrDefault("OLLAMA_CHAT_MODEL", "qwen3:8b"));
        int batchSize = Integer.getInteger("ollama.clean.batchSize", 10);
        Integer maxRows = Integer.getInteger("ollama.clean.maxRows");

        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();
        OllamaChatModel chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder().model(model).disableThinking().build())
                .build();
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        OllamaExcelJobPreprocessService service = new OllamaExcelJobPreprocessService(chatClient, new ObjectMapper());
        List<NormalizedJobRecord> records = service.preprocess(excelPath, "excel_ollama_qwen3", batchSize, maxRows);
        assertFalse(records.isEmpty(), "Expected preprocessing to produce at least one record");

        Path outputDir = Path.of("data", "processed", "jobs-ollama-qwen3");
        Path normalizedPath = service.exportJsonLines(records, outputDir);

        assertTrue(Files.exists(normalizedPath), "normalized.jsonl should exist");
        assertTrue(Files.exists(outputDir.resolve("rejected.jsonl")), "rejected.jsonl should exist");
        assertTrue(Files.exists(outputDir.resolve("review.jsonl")), "review.jsonl should exist");
        assertTrue(Files.exists(outputDir.resolve("summary.json")), "summary.json should exist");
    }
}
