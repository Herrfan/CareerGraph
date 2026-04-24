package com.zust.qyf.careeragent.domain.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthUserDTO(
        Long id,
        String username,
        String role,
        @JsonProperty("is_guest") boolean isGuest
) {
}
