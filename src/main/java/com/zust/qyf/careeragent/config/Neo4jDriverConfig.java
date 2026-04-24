package com.zust.qyf.careeragent.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
public class Neo4jDriverConfig {

    @Value("${spring.neo4j.uri:bolt://localhost:7687}")
    private String uri;

    @Value("${spring.neo4j.authentication.username:neo4j}")
    private String username;

    @Value("${spring.neo4j.authentication.password:12345678}")
    private String password;

    @Bean
    @Primary
    public Driver neo4jDriver() {
        Config config = Config.builder()
                .withConnectionTimeout(2, TimeUnit.SECONDS)
                .withConnectionAcquisitionTimeout(2, TimeUnit.SECONDS)
                .withMaxTransactionRetryTime(1, TimeUnit.SECONDS)
                .build();
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password), config);
    }
}
