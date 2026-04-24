package com.zust.qyf.careeragent.domain.dto.knowledge;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KnowledgeSnippetDTO(
        @JsonProperty("snippet_id") String snippetId,
        String title,
        @JsonProperty("source_type") String sourceType,
        @JsonProperty("source_id") String sourceId,
        String content,
        List<String> keywords,
        double score
) {
}
