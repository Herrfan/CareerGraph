package com.zust.qyf.careeragent.infrastructure.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.application.JobPortraitAssembler;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobCatalogServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void processedPortraitDirectoryBuildsNormalizedFamilyJob() throws IOException {
        Path processedDir = tempDir.resolve("processed");
        Files.createDirectories(processedDir);

        Files.writeString(processedDir.resolve("Java.json"), """
                {
                  "宀椾綅鍚嶇О": "Java",
                  "鍩庡競钖祫": {
                    "北京 - 海淀区": "1.5-3 万",
                    "杭州 - 滨江区": "1.4-2.5 万"
                  },
                  "涓撲笟鎶€鑳?": {
                    "编程语言 (Java/Spring Boot/MySQL)": ["了解", "掌握", "精通"]
                  },
                  "璇佷功瑕佹眰": {
                    "AWS 架构师认证": ["无", "助理级", "专业级"]
                  }
                }
                """, StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        JobCatalogService service = new JobCatalogService(
                new AbilityWeightsService(objectMapper),
                objectMapper,
                new JobPortraitAssembler(),
                processedDir,
                "file:" + tempDir.resolve("fallback").toAbsolutePath().toString().replace('\\', '/') + "/*.json"
        );

        List<JobProfileDTO> jobs = service.getJobs();

        assertEquals(1, jobs.size());
        JobProfileDTO job = jobs.get(0);
        assertEquals("Java开发", job.title());
        assertEquals("Java开发", job.category());
        assertEquals("portrait_family", job.portraitScope());
        assertTrue(job.requiredSkills().contains("Java"));
        assertEquals(List.of(processedDir.resolve("Java.json").toString()), job.sourceRecordIds());
    }
}
