package com.zust.qyf.careeragent.domain.dto.job;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobOptionDTO(
        @JsonProperty("title") String title,
        @JsonProperty("category") String category
) {
}
