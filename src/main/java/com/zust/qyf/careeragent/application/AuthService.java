package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.auth.AuthResponseDTO;
import com.zust.qyf.careeragent.domain.dto.auth.AuthUserDTO;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.UserAccountEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.UserRole;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.UserSessionEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.UserAccountRepository;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordHashService passwordHashService;

    @Value("${app.auth.session-days:30}")
    private long sessionDays;

    public AuthService(UserAccountRepository userAccountRepository,
                       UserSessionRepository userSessionRepository,
                       PasswordHashService passwordHashService) {
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordHashService = passwordHashService;
    }

    public AuthResponseDTO register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);
        if (userAccountRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }

        UserAccountEntity user = new UserAccountEntity();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordHashService.hash(password));
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        userAccountRepository.save(user);
        return issueSession(user);
    }

    public AuthResponseDTO login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        UserAccountEntity user = userAccountRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new IllegalArgumentException("账号或密码错误"));
        if (!passwordHashService.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return issueSession(user);
    }

    public Optional<AuthenticatedUser> resolveUser(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return userSessionRepository.findByToken(token)
                .filter(session -> !session.isRevoked())
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()))
                .flatMap(session -> userAccountRepository.findById(session.getUserId())
                        .map(user -> new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole())));
    }

    public Optional<AuthUserDTO> currentUser(String authorizationHeader) {
        return resolveUser(authorizationHeader)
                .map(user -> new AuthUserDTO(user.id(), user.username(), user.role().name(), false));
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null || token.isBlank()) {
            return;
        }
        userSessionRepository.findByToken(token).ifPresent(session -> {
            session.setRevoked(true);
            userSessionRepository.save(session);
        });
    }

    public void requireAdmin(String authorizationHeader) {
        AuthenticatedUser user = resolveUser(authorizationHeader)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        if (user.role() != UserRole.ADMIN) {
            throw new IllegalArgumentException("需要管理员权限");
        }
    }

    public Optional<UserAccountEntity> findByUsername(String username) {
        return userAccountRepository.findByUsername(normalizeUsername(username));
    }

    private AuthResponseDTO issueSession(UserAccountEntity user) {
        UserSessionEntity session = new UserSessionEntity();
        session.setToken(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(sessionDays));
        session.setRevoked(false);
        userSessionRepository.save(session);

        return new AuthResponseDTO(
                true,
                session.getToken(),
                new AuthUserDTO(user.getId(), user.getUsername(), user.getRole().name(), false)
        );
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        return username.trim().toLowerCase();
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少需要 6 位");
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        return authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7).trim() : authorizationHeader.trim();
    }

    public record AuthenticatedUser(Long id, String username, UserRole role) {
    }
}
