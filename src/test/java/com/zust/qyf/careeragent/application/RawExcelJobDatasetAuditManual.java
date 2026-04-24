package com.zust.qyf.careeragent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawExcelJobDatasetAuditManual {

    @Test
    void auditCurrentWorkspaceExcelAndWriteArtifacts() throws Exception {
        Path excelPath;
        try (var stream = Files.list(Path.of("."))) {
            excelPath = stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".xls") || name.endsWith(".xlsx");
                    })
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No Excel file found in workspace root"));
        }

        RawExcelJobPreprocessService service = new RawExcelJobPreprocessService(new ObjectMapper());
        Integer maxRows = Integer.getInteger("raw.clean.maxRows");
        String outputDirProperty = System.getProperty("raw.clean.outputDir");
        List<NormalizedJobRecord> records = service.preprocess(excelPath, "excel_audit", maxRows);
        assertFalse(records.isEmpty(), "Expected preprocessing to produce at least one record");

        Path outputDir = outputDirProperty == null || outputDirProperty.isBlank()
                ? Path.of("data", "processed", "jobs-audit")
                : Path.of(outputDirProperty);
        Path normalizedPath = service.exportJsonLines(records, outputDir);

        assertTrue(Files.exists(normalizedPath), "normalized.jsonl should exist");
        assertTrue(Files.exists(outputDir.resolve("rejected.jsonl")), "rejected.jsonl should exist");
        assertTrue(Files.exists(outputDir.resolve("review.jsonl")), "review.jsonl should exist");
        assertTrue(Files.exists(outputDir.resolve("summary.json")), "summary.json should exist");
    }
}
