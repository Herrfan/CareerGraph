package com.zust.qyf.careeragent.web;

import com.zust.qyf.careeragent.application.AuthService;
import com.zust.qyf.careeragent.domain.dto.auth.AuthResponseDTO;
import com.zust.qyf.careeragent.domain.dto.auth.AuthUserDTO;
import com.zust.qyf.careeragent.domain.dto.auth.LoginRequestDTO;
import com.zust.qyf.careeragent.domain.dto.auth.RegisterRequestDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponseDTO register(@RequestBody RegisterRequestDTO request) {
        return authService.register(request.username(), request.password());
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@RequestBody LoginRequestDTO request) {
        return authService.login(request.username(), request.password());
    }

    @GetMapping("/me")
    public AuthUserDTO me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.currentUser(authorization)
                .orElseThrow(() -> new IllegalArgumentException("未登录"));
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
    }
}
