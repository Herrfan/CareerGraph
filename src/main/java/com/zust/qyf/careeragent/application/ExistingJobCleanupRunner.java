package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.infrastructure.mysql.entity.NormalizedJobRecordEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.NormalizedJobRecordRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExistingJobCleanupRunner implements ApplicationRunner {
    private final NormalizedJobRecordRepository normalizedJobRecordRepository;
    private final OllamaJobCleanupService ollamaJobCleanupService;

    public ExistingJobCleanupRunner(NormalizedJobRecordRepository normalizedJobRecordRepository,
                                    OllamaJobCleanupService ollamaJobCleanupService) {
        this.normalizedJobRecordRepository = normalizedJobRecordRepository;
        this.ollamaJobCleanupService = ollamaJobCleanupService;
    }

    @Override
    public void run(ApplicationArguments args) {
        while (true) {
            List<NormalizedJobRecordEntity> candidates = normalizedJobRecordRepository.findCleanupCandidates(
                    "active",
                    "ready_v4",
                    PageRequest.of(0, 300)
            );
            if (candidates.isEmpty()) {
                break;
            }
            List<NormalizedJobRecordEntity> cleaned = candidates.stream()
                    .map(ollamaJobCleanupService::cleanupIfNeeded)
                    .toList();
            normalizedJobRecordRepository.saveAll(cleaned);
        }
    }
}
