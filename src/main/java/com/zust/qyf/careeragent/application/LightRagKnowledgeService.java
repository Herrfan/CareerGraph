package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.knowledge.KnowledgeSnippetDTO;
import com.zust.qyf.careeragent.infrastructure.knowledge.JobCatalogService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class LightRagKnowledgeService {
    private final JobCatalogService jobCatalogService;
    private final EmbeddingModel embeddingModel;
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();

    public LightRagKnowledgeService(JobCatalogService jobCatalogService,
                                    EmbeddingModel embeddingModel) {
        this.jobCatalogService = jobCatalogService;
        this.embeddingModel = embeddingModel;
    }

    public List<KnowledgeSnippetDTO> search(String query, String targetJob, int limit) {
        String normalizedQuery = normalize(query);
        String normalizedTarget = normalize(targetJob);
        float[] queryEmbedding = safeEmbed(query);

        return buildSnippets().stream()
                .map(snippet -> new KnowledgeSnippetDTO(
                        snippet.snippetId(),
                        snippet.title(),
                        snippet.sourceType(),
                        snippet.sourceId(),
                        snippet.content(),
                        snippet.keywords(),
                        roundScore(scoreSnippet(snippet, normalizedQuery, normalizedTarget, queryEmbedding))
                ))
                .filter(snippet -> snippet.score() > 0.12)
                .sorted(Comparator.comparingDouble(KnowledgeSnippetDTO::score).reversed())
                .limit(Math.max(limit, 1))
                .toList();
    }

    public String buildPromptContext(String query, String targetJob, int limit) {
        List<KnowledgeSnippetDTO> snippets = search(query, targetJob, limit);
        if (snippets.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("[LightRAG Knowledge]\n");
        for (int i = 0; i < snippets.size(); i++) {
            KnowledgeSnippetDTO snippet = snippets.get(i);
            builder.append("Snippet ").append(i + 1)
                    .append(" | title=").append(snippet.title())
                    .append(" | score=").append(snippet.score())
                    .append(" | source=").append(snippet.sourceType())
                    .append('\n');
            builder.append(snippet.content()).append('\n');
            builder.append("keywords=").append(String.join(", ", snippet.keywords())).append("\n\n");
        }
        return builder.toString().trim();
    }

    private List<KnowledgeSnippetDTO> buildSnippets() {
        List<JobProfileDTO> jobs = jobCatalogService.getJobs();
        List<KnowledgeSnippetDTO> snippets = new ArrayList<>();
        for (JobProfileDTO job : jobs) {
            snippets.add(buildOverviewSnippet(job));
            snippets.add(buildSkillSnippet(job));
            snippets.add(buildAbilitySnippet(job));
        }
        return snippets;
    }

    private KnowledgeSnippetDTO buildOverviewSnippet(JobProfileDTO job) {
        String content = "Role " + job.title()
                + " belongs to " + safe(job.category())
                + ", city tier " + safe(job.cityTier())
                + ", salary band " + safe(job.salaryBand())
                + ", source count " + (job.portraitSourceCount() == null ? 0 : job.portraitSourceCount())
                + ", confidence " + (job.portraitConfidence() == null ? 0.0 : job.portraitConfidence())
                + ". Description: " + safe(job.description());
        return new KnowledgeSnippetDTO(
                job.jobId() + "#overview",
                job.title(),
                "portrait_overview",
                job.jobId(),
                content,
                buildKeywords(job, List.of("overview", safe(job.cityTier()), safe(job.salaryBand()))),
                0.0
        );
    }

    private KnowledgeSnippetDTO buildSkillSnippet(JobProfileDTO job) {
        String content = "Role " + job.title()
                + " requires skills: " + String.join(", ", safeList(job.requiredSkills()))
                + ". Preferred certificates: " + String.join(", ", safeList(job.requiredCertificates()))
                + ". Experience requirement: " + safe(job.experienceRequired()) + ".";
        return new KnowledgeSnippetDTO(
                job.jobId() + "#skills",
                job.title(),
                "portrait_skills",
                job.jobId(),
                content,
                buildKeywords(job, merge(job.requiredSkills(), job.requiredCertificates())),
                0.0
        );
    }

    private KnowledgeSnippetDTO buildAbilitySnippet(JobProfileDTO job) {
        List<String> topAbilities = safeMap(job.abilityPriority()).entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(4)
                .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                .toList();

        String portraitText = safePortraitMap(job.abilityPortrait()).entrySet().stream()
                .limit(4)
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(" | "));

        String content = "Role " + job.title()
                + " values abilities " + String.join(", ", topAbilities)
                + ". Portrait evidence: " + portraitText;
        return new KnowledgeSnippetDTO(
                job.jobId() + "#abilities",
                job.title(),
                "portrait_abilities",
                job.jobId(),
                content,
                buildKeywords(job, topAbilities),
                0.0
        );
    }

    private double scoreSnippet(KnowledgeSnippetDTO snippet,
                                String normalizedQuery,
                                String normalizedTarget,
                                float[] queryEmbedding) {
        double lexical = 0.0;
        String normalizedTitle = normalize(snippet.title());
        String normalizedContent = normalize(snippet.content());

        if (!normalizedQuery.isBlank()) {
            if (normalizedTitle.contains(normalizedQuery) || normalizedQuery.contains(normalizedTitle)) {
                lexical += 1.2;
            }
            if (normalizedContent.contains(normalizedQuery)) {
                lexical += 0.9;
            }
            for (String keyword : snippet.keywords()) {
                String normalizedKeyword = normalize(keyword);
                if (!normalizedKeyword.isBlank()
                        && (normalizedKeyword.contains(normalizedQuery) || normalizedQuery.contains(normalizedKeyword))) {
                    lexical += 0.25;
                }
            }
        }

        if (!normalizedTarget.isBlank()
                && (normalizedTitle.contains(normalizedTarget) || normalizedTarget.contains(normalizedTitle))) {
            lexical += 0.45;
        }

        double semantic = cosineSimilarity(queryEmbedding, safeEmbed(snippet.content()));
        return lexical + semantic * 0.55;
    }

    private List<String> buildKeywords(JobProfileDTO job, List<String> extras) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        keywords.add(job.title());
        keywords.add(job.category());
        keywords.add(job.cityTier());
        keywords.add(job.salaryBand());
        keywords.addAll(safeList(job.requiredSkills()));
        keywords.addAll(safeList(extras));
        return keywords.stream().filter(this::notBlank).limit(12).toList();
    }

    private List<String> merge(List<String> left, List<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(safeList(left));
        merged.addAll(safeList(right));
        return new ArrayList<>(merged);
    }

    private float[] safeEmbed(String text) {
        if (!notBlank(text)) {
            return new float[0];
        }
        return embeddingCache.computeIfAbsent(text, key -> {
            try {
                return embeddingModel.embed(key);
            } catch (Exception e) {
                return new float[0];
            }
        });
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0 || left.length != right.length) {
            return 0.0;
        }

        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return Math.max(0.0, dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm)));
    }

    private Map<String, Integer> safeMap(Map<String, Integer> value) {
        return value == null ? Map.of() : value;
    }

    private Map<String, String> safePortraitMap(Map<String, String> value) {
        return value == null ? Map.of() : value;
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : value.stream().filter(this::notBlank).toList();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").replace("/", "").replace("+", "");
    }

    private double roundScore(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
