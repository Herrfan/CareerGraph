package com.zust.qyf.careeragent.infrastructure.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class NormalizedJobDataLoader {
    private final ObjectMapper objectMapper;

    public NormalizedJobDataLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<NormalizedJobRecord> loadJsonLines(Path path) {
        if (path == null || !Files.exists(path)) {
            throw new IllegalArgumentException("Normalized data file does not exist: " + path);
        }

        List<NormalizedJobRecord> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                records.add(objectMapper.readValue(trimmed, NormalizedJobRecord.class));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load normalized job data: " + path, e);
        }
        return records;
    }
}
