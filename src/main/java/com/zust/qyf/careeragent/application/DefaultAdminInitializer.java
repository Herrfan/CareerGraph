package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.infrastructure.mysql.entity.UserAccountEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.UserRole;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DefaultAdminInitializer implements ApplicationRunner {
    private final UserAccountRepository userAccountRepository;
    private final PasswordHashService passwordHashService;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    public DefaultAdminInitializer(UserAccountRepository userAccountRepository,
                                   PasswordHashService passwordHashService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHashService = passwordHashService;
    }

    @Override
    public void run(ApplicationArguments args) {
        userAccountRepository.findByUsername(adminUsername.trim().toLowerCase()).orElseGet(() -> {
            UserAccountEntity admin = new UserAccountEntity();
            admin.setUsername(adminUsername.trim().toLowerCase());
            admin.setPasswordHash(passwordHashService.hash(adminPassword));
            admin.setRole(UserRole.ADMIN);
            admin.setCreatedAt(LocalDateTime.now());
            return userAccountRepository.save(admin);
        });
    }
}
