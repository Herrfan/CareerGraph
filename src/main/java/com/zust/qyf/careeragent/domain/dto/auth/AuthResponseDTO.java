package com.zust.qyf.careeragent.domain.dto.auth;

public record AuthResponseDTO(
        boolean success,
        String token,
        AuthUserDTO user
) {
}
