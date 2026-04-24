package com.zust.qyf.careeragent.domain.dto.path;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record CareerPathResponseDTO(
        @JsonProperty("graph_paths") CareerPathNodeDTO careerPath,
        @JsonProperty("vector_similar_jobs") List<Map<String, Object>> similarJobs,
        @JsonProperty("vertical_path") List<Map<String, Object>> verticalPath,
        @JsonProperty("horizontal_path") List<Map<String, Object>> horizontalPath
) {
}
