package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.NormalizedJobRecordRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class RawJobImportBootstrapRunner implements ApplicationRunner {
    private final NormalizedJobRecordRepository normalizedJobRecordRepository;
    private final RawExcelJobPreprocessService rawExcelJobPreprocessService;
    private final EnterpriseKnowledgeImportApplicationService enterpriseKnowledgeImportApplicationService;

    public RawJobImportBootstrapRunner(NormalizedJobRecordRepository normalizedJobRecordRepository,
                                       RawExcelJobPreprocessService rawExcelJobPreprocessService,
                                       EnterpriseKnowledgeImportApplicationService enterpriseKnowledgeImportApplicationService) {
        this.normalizedJobRecordRepository = normalizedJobRecordRepository;
        this.rawExcelJobPreprocessService = rawExcelJobPreprocessService;
        this.enterpriseKnowledgeImportApplicationService = enterpriseKnowledgeImportApplicationService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (normalizedJobRecordRepository.countByStatus("active") > 0) {
            return;
        }

        Path rawExcel = findRawExcel();
        if (rawExcel == null) {
            return;
        }

        List<NormalizedJobRecord> records = rawExcelJobPreprocessService.preprocess(rawExcel, "excel_bootstrap");
        if (records.isEmpty()) {
            return;
        }

        Path output = rawExcelJobPreprocessService.exportJsonLines(records, Path.of("data", "processed", "jobs"));
        enterpriseKnowledgeImportApplicationService.importNormalizedRecords(records, output.toString());
    }

    private Path findRawExcel() throws Exception {
        try (var stream = Files.list(Path.of("."))) {
            return stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".xls") || name.endsWith(".xlsx");
                    })
                    .findFirst()
                    .orElse(null);
        }
    }
}
