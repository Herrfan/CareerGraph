package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import com.zust.qyf.careeragent.infrastructure.graph.EnterpriseGraphService;
import com.zust.qyf.careeragent.infrastructure.knowledge.NormalizedJobDataLoader;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnterpriseKnowledgeImportApplicationService {
    private final NormalizedJobDataLoader normalizedJobDataLoader;
    private final EnterpriseJobRecordPersistenceService enterpriseJobRecordPersistenceService;
    private final EnterpriseGraphService enterpriseGraphService;

    public EnterpriseKnowledgeImportApplicationService(NormalizedJobDataLoader normalizedJobDataLoader,
                                                       EnterpriseJobRecordPersistenceService enterpriseJobRecordPersistenceService,
                                                       EnterpriseGraphService enterpriseGraphService) {
        this.normalizedJobDataLoader = normalizedJobDataLoader;
        this.enterpriseJobRecordPersistenceService = enterpriseJobRecordPersistenceService;
        this.enterpriseGraphService = enterpriseGraphService;
    }

    public Map<String, Object> importNormalizedJobs(String filePath) {
        List<NormalizedJobRecord> records = normalizedJobDataLoader.loadJsonLines(Path.of(filePath));
        return importNormalizedRecords(records, filePath);
    }

    public Map<String, Object> importNormalizedRecords(List<NormalizedJobRecord> records, String sourceFile) {
        long validCount = records.stream().filter(NormalizedJobRecord::isImportable).count();
        int mysqlImportedCount = enterpriseJobRecordPersistenceService.upsertRecords(records);
        int graphImportedCount;
        try {
            graphImportedCount = enterpriseGraphService.importJobs(records);
        } catch (Exception e) {
            graphImportedCount = 0;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_records", records.size());
        result.put("valid_records", validCount);
        result.put("mysql_upserted_records", mysqlImportedCount);
        result.put("graph_upserted_records", graphImportedCount);
        result.put("source_file", sourceFile);
        return result;
    }
}
