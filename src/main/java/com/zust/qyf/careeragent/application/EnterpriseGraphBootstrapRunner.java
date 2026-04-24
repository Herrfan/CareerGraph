package com.zust.qyf.careeragent.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class EnterpriseGraphBootstrapRunner implements ApplicationRunner {
    private final EnterpriseGraphBootstrapApplicationService enterpriseGraphBootstrapApplicationService;

    @Value("${app.graph.bootstrap-on-startup:true}")
    private boolean bootstrapOnStartup;

    public EnterpriseGraphBootstrapRunner(EnterpriseGraphBootstrapApplicationService enterpriseGraphBootstrapApplicationService) {
        this.enterpriseGraphBootstrapApplicationService = enterpriseGraphBootstrapApplicationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapOnStartup) {
            return;
        }
        enterpriseGraphBootstrapApplicationService.bootstrapFromMysql(false);
        enterpriseGraphBootstrapApplicationService.bootstrapFromPortraits(false);
    }
}
