package com.zust.qyf.careeragent.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OllamaExcelJobPreprocessService {
    private static final double REVIEW_CONFIDENCE_THRESHOLD = 0.78;
    private static final int DEFAULT_PROMPT_DESCRIPTION_LIMIT = 900;
    private static final int DEFAULT_PROMPT_COMPANY_INTRO_LIMIT = 160;
    private static final int DEFAULT_BATCH_NUM_PREDICT = 2400;
    private static final int DEFAULT_SINGLE_NUM_PREDICT = 800;
    private static final String NON_DIGITAL_JOB = "非信息化岗位";

    private static final List<String> ALLOWED_CATEGORIES = List.of(
            "Java开发", "前端开发", "Python开发", "算法工程师", "数据分析",
            "软件测试", "硬件测试", "实施工程师", "技术支持工程师",
            "运维/DevOps", "产品/项目经理", "APP推广/运营", "科研人员",
            "综合开发", NON_DIGITAL_JOB
    );

    private static final List<String> COMPANY_HEADERS = List.of("company_name", "company", "公司全称", "公司名称", "企业名称", "公司");
    private static final List<String> TITLE_HEADERS = List.of("job_title", "title", "职位名称", "岗位名称", "职位", "岗位");
    private static final List<String> ADDRESS_HEADERS = List.of("work_address", "address", "工作地址", "工作地点", "地点", "地址");
    private static final List<String> SALARY_HEADERS = List.of("salary", "薪资范围", "薪资", "工资", "薪酬");
    private static final List<String> INDUSTRY_HEADERS = List.of("industry", "所属行业", "行业");
    private static final List<String> COMPANY_SIZE_HEADERS = List.of("company_size", "人员规模", "公司规模", "企业规模");
    private static final List<String> COMPANY_TYPE_HEADERS = List.of("company_type", "企业性质", "公司性质", "企业类型");
    private static final List<String> JOB_CODE_HEADERS = List.of("job_code", "职位编码", "岗位编码", "职位id", "岗位id");
    private static final List<String> DESCRIPTION_HEADERS = List.of("job_description", "description", "职位描述", "岗位描述", "岗位职责", "职位详情");
    private static final List<String> COMPANY_INTRO_HEADERS = List.of("company_intro", "company_profile", "公司简介", "企业简介", "公司介绍");
    private static final List<String> EDUCATION_HEADERS = List.of("education", "学历", "学历要求");
    private static final List<String> EXPERIENCE_HEADERS = List.of("experience", "经验", "工作经验", "经验要求");
    private static final List<String> URL_HEADERS = List.of("source_url", "url", "链接", "职位链接");
    private static final List<String> PUBLISHED_AT_HEADERS = List.of("published_at", "发布时间", "发布日期");
    private static final List<String> SOURCE_ID_HEADERS = List.of("source_id", "id", "职位id", "岗位id");

    private static final Pattern CITY_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,8})(?:市)?(?:[-/·\\s]?([\\u4e00-\\u9fa5]{1,12}(?:区|县|镇|新区|开发区)))?");
    private static final Pattern EDUCATION_PATTERN = Pattern.compile("(博士|硕士|研究生|本科|大专|专科|中专|高中)(?:及以上|以上|及以下|以下)?");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d+\\s*(?:-|~|到|至)\\s*\\d+\\s*年|\\d+\\s*年(?:以上)?|应届(?:毕业生)?|实习(?:生)?|经验不限|无经验)");
    private static final Pattern JOB_CODE_PATTERN = Pattern.compile("(CC[A-Z0-9]+|[A-Z]{2,8}\\d[A-Z0-9_-]{4,})");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    private final ChatClient ollamaChatClient;
    private final ObjectMapper objectMapper;
    private final boolean batchCleanupEnabled;
    private final int promptDescriptionLimit;
    private final int promptCompanyIntroLimit;
    private final int batchNumPredict;
    private final int singleNumPredict;

    public OllamaExcelJobPreprocessService(ChatClient ollamaChatClient, ObjectMapper objectMapper) {
        this.ollamaChatClient = ollamaChatClient;
        this.objectMapper = objectMapper;
        this.batchCleanupEnabled = readBoolean("ollama.clean.batchEnabled", "OLLAMA_CLEAN_BATCH_ENABLED", true);
        this.promptDescriptionLimit = readPositiveInt(
                "ollama.clean.promptDescriptionLimit",
                "OLLAMA_CLEAN_PROMPT_DESCRIPTION_LIMIT",
                DEFAULT_PROMPT_DESCRIPTION_LIMIT
        );
        this.promptCompanyIntroLimit = readPositiveInt(
                "ollama.clean.promptCompanyIntroLimit",
                "OLLAMA_CLEAN_PROMPT_COMPANY_INTRO_LIMIT",
                DEFAULT_PROMPT_COMPANY_INTRO_LIMIT
        );
        this.batchNumPredict = readPositiveInt(
                "ollama.clean.batchNumPredict",
                "OLLAMA_CLEAN_BATCH_NUM_PREDICT",
                DEFAULT_BATCH_NUM_PREDICT
        );
        this.singleNumPredict = readPositiveInt(
                "ollama.clean.singleNumPredict",
                "OLLAMA_CLEAN_SINGLE_NUM_PREDICT",
                DEFAULT_SINGLE_NUM_PREDICT
        );
    }

    public List<NormalizedJobRecord> preprocess(Path inputPath, String sourcePlatform, int batchSize, Integer maxRows) {
        if (inputPath == null || !Files.exists(inputPath)) {
            return List.of();
        }

        List<RawJobSeed> seeds = readSeeds(inputPath, sourcePlatform, maxRows);
        if (seeds.isEmpty()) {
            return List.of();
        }

        int safeBatchSize = Math.max(1, batchSize);
        List<NormalizedJobRecord> records = new ArrayList<>();
        for (int start = 0; start < seeds.size(); start += safeBatchSize) {
            int end = Math.min(start + safeBatchSize, seeds.size());
            List<RawJobSeed> batch = seeds.subList(start, end);
            records.addAll(cleanBatch(batch));
            System.out.printf(Locale.ROOT, "LLM cleaned rows %d-%d / %d%n", start + 1, end, seeds.size());
        }
        return deduplicate(records);
    }

    public Path exportJsonLines(List<NormalizedJobRecord> records, Path outputDir) {
        try {
            Files.createDirectories(outputDir);
            List<NormalizedJobRecord> valid = records.stream().filter(NormalizedJobRecord::isImportable).toList();
            List<NormalizedJobRecord> rejected = records.stream().filter(record -> !record.isImportable()).toList();
            List<NormalizedJobRecord> review = valid.stream()
                    .filter(record -> record.confidenceScore() != null && record.confidenceScore() < REVIEW_CONFIDENCE_THRESHOLD)
                    .toList();

            Path normalizedPath = outputDir.resolve("normalized.jsonl");
            writeJsonLines(normalizedPath, valid);
            writeJsonLines(outputDir.resolve("rejected.jsonl"), rejected);
            writeJsonLines(outputDir.resolve("review.jsonl"), review);
            writeSummary(outputDir.resolve("summary.json"), records, valid, rejected, review);
            return normalizedPath;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export cleaned jobs", e);
        }
    }

    private NormalizedJobRecord buildLlmFailedRecord(RawJobSeed seed) {
        return new NormalizedJobRecord(
                seed.sourceId,
                seed.sourcePlatform,
                seed.sourceUrl,
                seed.rowHash,
                trimLength(cleanText(seed.companyName), 255),
                trimLength(cleanText(seed.companySize), 128),
                trimLength(cleanText(seed.companyType), 128),
                "",
                trimLength(cleanText(seed.jobTitle), 255),
                trimLength(cleanText(seed.jobCode), 128),
                "",
                trimLength(cleanText(seed.industry), 255),
                trimLength(cleanText(seed.workAddress), 255),
                extractCity(seed.workAddress),
                trimLength(cleanText(seed.salaryText), 128),
                null,
                null,
                normalizeEducation(seed.educationLevel),
                normalizeExperience(seed.experienceText),
                "",
                List.of(),
                List.of("无"),
                trimLength(cleanText(seed.publishedAt), 64),
                LocalDateTime.now().toString(),
                false,
                "llm_cleanup_failed",
                0.01,
                0.0,
                "ollama_llm_failed"
        );
    }

    private List<RawJobSeed> readSeeds(Path inputPath, String sourcePlatform, Integer maxRows) {
        List<RawJobSeed> seeds = new ArrayList<>();
        DataFormatter formatter = new DataFormatter(Locale.SIMPLIFIED_CHINESE);
        try (InputStream inputStream = Files.newInputStream(inputPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return List.of();
            }

            Map<Integer, String> headers = readHeaders(sheet.getRow(0), formatter);
            int processed = 0;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                if (maxRows != null && processed >= maxRows) {
                    break;
                }
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = extractValues(row, headers, formatter);
                RawJobSeed seed = buildSeed(values, rowIndex, sourcePlatform);
                if (seed != null) {
                    seeds.add(seed);
                    processed++;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read raw Excel jobs: " + inputPath, e);
        }
        return seeds;
    }

    private List<NormalizedJobRecord> cleanBatch(List<RawJobSeed> batch) {
        if (batchCleanupEnabled && batch.size() > 1) {
            try {
                List<CleanedPayload> payloads = requestBatchCleanup(batch);
                if (payloads.size() == batch.size()) {
                    List<NormalizedJobRecord> results = new ArrayList<>();
                    for (int i = 0; i < batch.size(); i++) {
                        results.add(mapToRecord(batch.get(i), payloads.get(i), "ollama_qwen3_batch_v2"));
                    }
                    return results;
                }
                System.out.printf(Locale.ROOT,
                        "Batch cleanup returned %d payloads for %d rows, falling back to single-row cleanup.%n",
                        payloads.size(),
                        batch.size());
            } catch (Exception exception) {
                System.out.printf(Locale.ROOT, "Batch cleanup failed, falling back to single-row cleanup: %s%n", exception.getMessage());
            }
        }

        List<NormalizedJobRecord> results = new ArrayList<>();
        for (RawJobSeed seed : batch) {
            try {
                CleanedPayload payload = requestSingleCleanup(seed);
                results.add(mapToRecord(seed, payload, "ollama_qwen3_single_v2"));
            } catch (Exception exception) {
                System.out.printf(Locale.ROOT, "Single-row cleanup failed for %s, marking as llm_cleanup_failed: %s%n", seed.sourceId, exception.getMessage());
                results.add(buildLlmFailedRecord(seed));
            }
        }
        return results;
    }

    private List<CleanedPayload> requestBatchCleanup(List<RawJobSeed> batch) throws IOException {
        String schema = """
                [
                  {
                    "source_id": "",
                    "company_name": "",
                    "job_title": "",
                    "job_category": "",
                    "industry": "",
                    "company_size": "",
                    "company_type": "",
                    "work_address": "",
                    "city": "",
                    "salary_text": "",
                    "education_level": "",
                    "experience_text": "",
                    "job_code": "",
                    "job_description": "",
                    "company_intro": "",
                    "skills": [],
                    "required_certificates": [],
                    "published_at": "",
                    "is_valid": true,
                    "invalid_reason": "",
                    "confidence_score": 0.0
                  }
                ]
                """;

        String rawJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(batch.stream().map(this::toPromptPayload).toList());
        String prompt = """
                你正在为“基于AI的大学生职业规划智能体”清洗企业岗位数据。
                这些数据后续要用于岗位画像、岗位关联图谱、人岗匹配和职业发展报告。

                你的目标：
                1. 提取明确事实，不猜测。
                2. job_description 只保留岗位职责、岗位要求、技能要求等有效内容。
                3. 删除公司宣传、福利堆砌、投递引导、时间戳、追踪参数、纯噪声。
                4. company_intro 只保留简洁公司背景。
                5. skills 只保留明确出现的专业技能或工具，最多 8 个。
                6. required_certificates 只保留明确要求的证书；没有就返回 ["无"]。
                7. 只接受以下岗位类别：%s
                8. 如果岗位明显不是计算机/信息化/数字化相关岗位，job_category 必须设为“%s”，并将 is_valid=false、invalid_reason="non_digital_job"。
                9. 对于字段缺失的记录，不要删除，也不要脑补；保留空值并降低 confidence_score。
                10. confidence_score 取值 0 到 1。
                11. 必须按输入顺序返回 JSON 数组，只输出 JSON，不要解释，不要 Markdown。

                输入数据：
                %s

                输出 JSON 模式：
                %s
                """.formatted(String.join("、", ALLOWED_CATEGORIES), NON_DIGITAL_JOB, rawJson, schema);

        String response = ollamaChatClient.prompt()
                .system("你是严格的岗位数据清洗助手，只能返回合法 JSON。")
                .user(prompt)
                .options(OllamaChatOptions.builder()
                        .disableThinking()
                        .format("json")
                        .truncate(true)
                        .numPredict(batchNumPredict)
                        .build())
                .call()
                .content();

        JsonNode root = objectMapper.readTree(extractJsonPayload(response));
        if (!root.isArray()) {
            throw new IllegalArgumentException("Expected JSON array from Ollama");
        }
        List<CleanedPayload> payloads = new ArrayList<>();
        for (JsonNode item : root) {
            payloads.add(toPayload(item));
        }
        return payloads;
    }

    private CleanedPayload requestSingleCleanup(RawJobSeed seed) throws IOException {
        String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toPromptPayload(seed));
        String prompt = """
                你正在为“基于AI的大学生职业规划智能体”清洗单条岗位数据。
                返回一个 JSON 对象，不要解释，不要 Markdown。

                规则：
                1. 保留公司名、岗位名、地址、薪资、学历、经验、岗位职责、岗位要求等事实。
                2. 删除公司宣传、福利、无关链接、时间戳和营销文案。
                3. job_description 仅保留岗位职责和要求。
                4. skills 最多 8 个，只能填明确提到的技能。
                5. required_certificates 没有则返回 ["无"]。
                6. 岗位类别只能从以下集合中选择：%s
                7. 如果不是信息化相关岗位，设置 job_category="%s" 且 is_valid=false。
                8. 对字段缺失的记录保留空值，不要删除，也不要自行补全。
                9. confidence_score 范围 0 到 1。

                输入：
                %s
                """.formatted(String.join("、", ALLOWED_CATEGORIES), NON_DIGITAL_JOB, payload);

        String response = ollamaChatClient.prompt()
                .system("你是严格的岗位数据清洗助手，只能返回合法 JSON。")
                .user(prompt)
                .options(OllamaChatOptions.builder()
                        .disableThinking()
                        .format("json")
                        .truncate(true)
                        .numPredict(singleNumPredict)
                        .build())
                .call()
                .content();
        return toPayload(objectMapper.readTree(extractJsonPayload(response)));
    }

    private NormalizedJobRecord mapToRecord(RawJobSeed seed, CleanedPayload payload, String cleanSource) {
        String companyName = firstNonBlank(text(payload == null ? null : payload.companyName), seed.companyName);
        String jobTitle = firstNonBlank(text(payload == null ? null : payload.jobTitle), seed.jobTitle);
        String industry = firstNonBlank(text(payload == null ? null : payload.industry), seed.industry);
        String companySize = firstNonBlank(text(payload == null ? null : payload.companySize), seed.companySize);
        String companyType = firstNonBlank(text(payload == null ? null : payload.companyType), seed.companyType);
        String workAddress = trimLength(cleanText(firstNonBlank(text(payload == null ? null : payload.workAddress), seed.workAddress)), 160);
        String salaryText = trimLength(cleanText(firstNonBlank(text(payload == null ? null : payload.salaryText), seed.salaryText)), 120);
        String educationLevel = firstNonBlank(
                text(payload == null ? null : payload.educationLevel),
                normalizeEducation(seed.educationLevel),
                normalizeEducation(seed.jobDescription)
        );
        String experienceText = firstNonBlank(
                text(payload == null ? null : payload.experienceText),
                normalizeExperience(seed.experienceText),
                normalizeExperience(seed.jobDescription)
        );
        String jobCode = firstNonBlank(text(payload == null ? null : payload.jobCode), seed.jobCode, extractJobCode(seed.sourceUrl + " " + seed.jobDescription));
        String jobDescription = trimLength(cleanText(firstNonBlank(text(payload == null ? null : payload.jobDescription), seed.jobDescription)), 2400);
        String companyIntro = trimLength(cleanText(firstNonBlank(text(payload == null ? null : payload.companyIntro), seed.companyIntro)), 1200);
        List<String> skills = cleanList(payload == null ? List.of() : payload.skills, extractSkillsFromText(jobTitle + " " + jobDescription));
        List<String> certificates = cleanCertificates(payload == null ? List.of() : payload.requiredCertificates);
        String category = normalizeCategory(firstNonBlank(text(payload == null ? null : payload.jobCategory), inferCategoryFallback(jobTitle, jobDescription, skills)));
        String city = firstNonBlank(text(payload == null ? null : payload.city), extractCity(workAddress), extractCity(jobDescription));
        String publishedAt = firstNonBlank(text(payload == null ? null : payload.publishedAt), seed.publishedAt);
        Integer[] salaryRange = parseSalary(salaryText);

        boolean valid = payload == null || payload.isValid == null || payload.isValid;
        String invalidReason = text(payload == null ? null : payload.invalidReason);
        if (blank(companyName)) {
            valid = false;
            invalidReason = "missing_company_name";
        } else if (blank(jobTitle)) {
            valid = false;
            invalidReason = "missing_job_title";
        } else if (NON_DIGITAL_JOB.equals(category)) {
            valid = false;
            invalidReason = "non_digital_job";
        } else if (blank(invalidReason)) {
            invalidReason = "";
        }

        double completeness = fieldCompletenessScore(
                companyName,
                jobTitle,
                industry,
                workAddress,
                city,
                salaryText,
                educationLevel,
                experienceText,
                jobDescription,
                skills,
                certificates
        );
        double confidence = payload == null || payload.confidenceScore == null || payload.confidenceScore <= 0
                ? heuristicConfidence(companyName, jobTitle, jobDescription, city, salaryRange[0], skills, valid, completeness)
                : clamp(payload.confidenceScore);

        return new NormalizedJobRecord(
                seed.sourceId,
                seed.sourcePlatform,
                seed.sourceUrl,
                seed.rowHash,
                companyName,
                companySize,
                companyType,
                companyIntro,
                jobTitle,
                jobCode,
                category,
                industry,
                workAddress,
                city,
                salaryText,
                salaryRange[0],
                salaryRange[1],
                educationLevel,
                experienceText,
                jobDescription,
                skills,
                certificates,
                publishedAt,
                LocalDateTime.now().toString(),
                valid,
                invalidReason,
                confidence,
                completeness,
                cleanSource
        );
    }

    private RawJobSeed buildSeed(Map<String, String> row, int index, String sourcePlatform) {
        String companyName = firstValue(row, COMPANY_HEADERS);
        String jobTitle = firstValue(row, TITLE_HEADERS);
        if (blank(companyName) && blank(jobTitle)) {
            return null;
        }

        String description = buildRawDescription(row);
        String sourceId = firstNonBlank(firstValue(row, SOURCE_ID_HEADERS), sourcePlatform + "-" + index);
        return new RawJobSeed(
                sourceId,
                sourcePlatform,
                trimLength(cleanText(companyName), 255),
                trimLength(cleanText(jobTitle), 255),
                trimLength(cleanText(firstValue(row, INDUSTRY_HEADERS)), 255),
                trimLength(cleanText(firstValue(row, COMPANY_SIZE_HEADERS)), 128),
                trimLength(cleanText(firstValue(row, COMPANY_TYPE_HEADERS)), 128),
                trimLength(cleanText(firstValue(row, ADDRESS_HEADERS)), 255),
                trimLength(cleanText(firstValue(row, SALARY_HEADERS)), 128),
                trimLength(cleanText(firstValue(row, EDUCATION_HEADERS)), 64),
                trimLength(cleanText(firstValue(row, EXPERIENCE_HEADERS)), 128),
                trimLength(cleanText(firstValue(row, JOB_CODE_HEADERS)), 128),
                trimLength(cleanText(description), 2400),
                trimLength(cleanText(firstValue(row, COMPANY_INTRO_HEADERS)), 1200),
                trimLength(cleanText(firstValue(row, URL_HEADERS)), 512),
                trimLength(cleanText(firstValue(row, PUBLISHED_AT_HEADERS)), 64),
                buildRowHash(row)
        );
    }

    private String buildRawDescription(Map<String, String> row) {
        String explicitDescription = firstValue(row, DESCRIPTION_HEADERS);
        if (!blank(explicitDescription)) {
            return explicitDescription;
        }
        List<String> fragments = new ArrayList<>();
        for (String value : row.values()) {
            String cleaned = cleanText(value);
            if (cleaned.length() >= 12 && !URL_PATTERN.matcher(cleaned).find()) {
                fragments.add(cleaned);
            }
        }
        return String.join("；", fragments);
    }

    private Map<String, Object> toPromptPayload(RawJobSeed seed) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source_id", seed.sourceId);
        payload.put("company_name", promptText(seed.companyName, 255));
        payload.put("job_title", promptText(seed.jobTitle, 255));
        payload.put("industry", promptText(seed.industry, 255));
        payload.put("company_size", promptText(seed.companySize, 128));
        payload.put("company_type", promptText(seed.companyType, 128));
        payload.put("work_address", promptText(seed.workAddress, 255));
        payload.put("salary_text", promptText(seed.salaryText, 128));
        payload.put("education_level", promptText(seed.educationLevel, 64));
        payload.put("experience_text", promptText(seed.experienceText, 128));
        payload.put("job_code", promptText(seed.jobCode, 128));
        payload.put("job_description", promptText(seed.jobDescription, promptDescriptionLimit));
        payload.put("company_intro", promptText(seed.companyIntro, promptCompanyIntroLimit));
        payload.put("source_url", promptText(seed.sourceUrl, 256));
        payload.put("published_at", promptText(seed.publishedAt, 64));
        payload.put("row_hash", seed.rowHash);
        return payload;
    }

    private void writeJsonLines(Path output, List<NormalizedJobRecord> records) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            for (NormalizedJobRecord record : records) {
                writer.write(objectMapper.writeValueAsString(record));
                writer.newLine();
            }
        }
    }

    private void writeSummary(Path output,
                              List<NormalizedJobRecord> records,
                              List<NormalizedJobRecord> valid,
                              List<NormalizedJobRecord> rejected,
                              List<NormalizedJobRecord> review) throws IOException {
        Map<String, Long> rejectionReasons = rejected.stream()
                .map(NormalizedJobRecord::invalidReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .collect(Collectors.groupingBy(reason -> reason, LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> categoryDistribution = valid.stream()
                .map(record -> blank(record.jobCategory()) ? "未分类" : record.jobCategory())
                .collect(Collectors.groupingBy(category -> category, LinkedHashMap::new, Collectors.counting()));

        Map<String, Long> cleanSourceDistribution = records.stream()
                .map(record -> blank(record.cleanSource()) ? "unknown" : record.cleanSource())
                .collect(Collectors.groupingBy(source -> source, LinkedHashMap::new, Collectors.counting()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("total", records.size());
        payload.put("valid", valid.size());
        payload.put("rejected", rejected.size());
        payload.put("review", review.size());
        payload.put("top_rejection_reasons", rejectionReasons.entrySet().stream().limit(10).map(entry -> List.of(entry.getKey(), entry.getValue())).toList());
        payload.put("category_distribution", categoryDistribution);
        payload.put("clean_source_distribution", cleanSourceDistribution);
        payload.put("generated_at", LocalDateTime.now().toString());

        Files.writeString(output, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload), StandardCharsets.UTF_8);
    }

    private Map<Integer, String> readHeaders(Row headerRow, DataFormatter formatter) {
        Map<Integer, String> headers = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            headers.put(cell.getColumnIndex(), normalizeHeader(formatter.formatCellValue(cell)));
        }
        return headers;
    }

    private Map<String, String> extractValues(Row row, Map<Integer, String> headers, DataFormatter formatter) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headers.entrySet()) {
            Cell cell = row.getCell(entry.getKey());
            values.put(entry.getValue(), cell == null ? "" : cleanText(formatter.formatCellValue(cell)));
        }
        return values;
    }

    private String normalizeHeader(String header) {
        return cleanText(header).toLowerCase(Locale.ROOT).replace("：", "").replace(":", "");
    }

    private String firstValue(Map<String, String> row, List<String> candidates) {
        for (String candidate : candidates) {
            String value = row.get(candidate.toLowerCase(Locale.ROOT));
            if (!blank(value)) {
                return value;
            }
        }
        return "";
    }

    private List<NormalizedJobRecord> deduplicate(List<NormalizedJobRecord> records) {
        Set<String> seen = new LinkedHashSet<>();
        List<NormalizedJobRecord> result = new ArrayList<>();
        for (NormalizedJobRecord record : records) {
            String key = record.sourceId() + "|" + record.companyName() + "|" + record.jobTitle() + "|" + record.city() + "|" + record.rowHash();
            if (seen.add(key)) {
                result.add(record);
            }
        }
        return result;
    }

    private String extractJsonPayload(String content) {
        String text = content == null ? "" : content.trim();
        int arrayStart = text.indexOf('[');
        int arrayEnd = text.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return text.substring(arrayStart, arrayEnd + 1);
        }
        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return text.substring(objectStart, objectEnd + 1);
        }
        throw new IllegalArgumentException("No JSON payload found in Ollama response");
    }

    private CleanedPayload toPayload(JsonNode node) {
        CleanedPayload payload = new CleanedPayload();
        payload.sourceId = text(node, "source_id");
        payload.companyName = text(node, "company_name");
        payload.jobTitle = text(node, "job_title");
        payload.jobCategory = text(node, "job_category");
        payload.industry = text(node, "industry");
        payload.companySize = text(node, "company_size");
        payload.companyType = text(node, "company_type");
        payload.workAddress = text(node, "work_address");
        payload.city = text(node, "city");
        payload.salaryText = text(node, "salary_text");
        payload.educationLevel = text(node, "education_level");
        payload.experienceText = text(node, "experience_text");
        payload.jobCode = text(node, "job_code");
        payload.jobDescription = text(node, "job_description");
        payload.companyIntro = text(node, "company_intro");
        payload.skills = array(node, "skills");
        payload.requiredCertificates = array(node, "required_certificates");
        payload.publishedAt = text(node, "published_at");
        payload.isValid = node.path("is_valid").asBoolean(true);
        payload.invalidReason = text(node, "invalid_reason");
        payload.confidenceScore = node.has("confidence_score") ? node.path("confidence_score").asDouble(0.0) : 0.0;
        return payload;
    }

    private String text(JsonNode node, String field) {
        if (node == null) {
            return "";
        }
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : cleanText(value.asText(""));
    }

    private List<String> array(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isArray()) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (JsonNode item : value) {
            String text = cleanText(item.asText(""));
            if (!blank(text)) {
                items.add(text);
            }
        }
        return items;
    }

    private List<String> cleanList(List<String> primary, List<String> fallback) {
        List<String> values = new ArrayList<>();
        values.addAll(primary == null ? List.of() : primary);
        if (values.isEmpty() && fallback != null) {
            values.addAll(fallback);
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList();
    }

    private List<String> cleanCertificates(List<String> values) {
        List<String> cleaned = values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .limit(8)
                .toList();
        return cleaned.isEmpty() ? List.of("无") : cleaned;
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("<br>", "；")
                .replace("<br/>", "；")
                .replace("<br />", "；")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace('\u3000', ' ')
                .replaceAll("[\\p{Cc}\\p{Cf}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeEducation(String text) {
        Matcher matcher = EDUCATION_PATTERN.matcher(cleanText(text));
        if (!matcher.find()) {
            return "";
        }
        return switch (matcher.group(1)) {
            case "研究生" -> "硕士";
            case "专科" -> "大专";
            default -> matcher.group(1);
        };
    }

    private String normalizeExperience(String text) {
        Matcher matcher = EXPERIENCE_PATTERN.matcher(cleanText(text));
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", "") : "";
    }

    private String extractJobCode(String text) {
        Matcher matcher = JOB_CODE_PATTERN.matcher(cleanText(text));
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractCity(String text) {
        Matcher matcher = CITY_PATTERN.matcher(cleanText(text));
        return matcher.find() ? matcher.group(1) : "";
    }

    private Integer[] parseSalary(String salaryText) {
        if (blank(salaryText)) {
            return new Integer[]{null, null};
        }
        String normalized = salaryText.toLowerCase(Locale.ROOT)
                .replace("k", "000")
                .replace("千", "000")
                .replace("万", "0000")
                .replace("元/月", "")
                .replace("/月", "")
                .replace("元", "");

        Matcher matcher = Pattern.compile("\\d+").matcher(normalized);
        List<Integer> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
            if (numbers.size() == 2) {
                break;
            }
        }
        if (numbers.isEmpty()) {
            return new Integer[]{null, null};
        }
        if (numbers.size() == 1) {
            return new Integer[]{numbers.get(0), numbers.get(0)};
        }
        return new Integer[]{Math.min(numbers.get(0), numbers.get(1)), Math.max(numbers.get(0), numbers.get(1))};
    }

    private boolean hasMeaningfulDescription(String description) {
        String cleaned = cleanText(description);
        if (cleaned.length() < 20) {
            return false;
        }
        String normalized = cleaned.toLowerCase(Locale.ROOT);
        if (containsAny(normalized,
                "职责", "要求", "负责", "参与", "开发", "测试", "维护", "部署", "实施", "支持",
                "分析", "设计", "优化", "系统", "项目", "数据库", "前端", "后端", "软件", "技术",
                "java", "python", "sql", "mysql", "oracle", "gis", "arcgis")) {
            return true;
        }
        return !containsAny(cleaned,
                "五险一金", "周末双休", "定期团建", "节日福利", "生日福利",
                "带薪年假", "免费食宿", "餐补", "房补", "商业保险");
    }

    private double heuristicConfidence(String companyName,
                                       String jobTitle,
                                       String description,
                                       String city,
                                       Integer salaryMin,
                                       List<String> skills,
                                       boolean valid,
                                       double completeness) {
        double score = 0.0;
        if (!blank(companyName)) score += 0.15;
        if (!blank(jobTitle)) score += 0.15;
        if (!blank(description) && description.length() >= 80) score += 0.25;
        if (!blank(city)) score += 0.1;
        if (salaryMin != null) score += 0.1;
        if (skills != null && !skills.isEmpty()) score += Math.min(0.2, skills.size() * 0.03);
        if (valid) score += 0.05;
        score = score * 0.8 + clamp(completeness) * 0.2;
        return Math.round(Math.min(score, 0.99) * 10000.0) / 10000.0;
    }

    private double fieldCompletenessScore(String companyName,
                                          String jobTitle,
                                          String industry,
                                          String workAddress,
                                          String city,
                                          String salaryText,
                                          String educationLevel,
                                          String experienceText,
                                          String jobDescription,
                                          List<String> skills,
                                          List<String> certificates) {
        int total = 11;
        int filled = 0;
        if (!blank(companyName)) filled++;
        if (!blank(jobTitle)) filled++;
        if (!blank(industry)) filled++;
        if (!blank(workAddress)) filled++;
        if (!blank(city)) filled++;
        if (!blank(salaryText)) filled++;
        if (!blank(educationLevel)) filled++;
        if (!blank(experienceText)) filled++;
        if (!blank(jobDescription)) filled++;
        if (skills != null && !skills.isEmpty()) filled++;
        if (certificates != null && !certificates.isEmpty() && !(certificates.size() == 1 && "无".equals(certificates.get(0)))) filled++;
        return Math.round((filled * 1.0 / total) * 10000.0) / 10000.0;
    }

    private String inferCategoryFallback(String jobTitle, String description, List<String> skills) {
        String title = cleanText(jobTitle).toLowerCase(Locale.ROOT);
        String text = (cleanText(jobTitle) + " " + cleanText(description) + " " + String.join(" ", skills)).toLowerCase(Locale.ROOT);
        if (containsAny(title, "实施", "交付")) return "实施工程师";
        if (containsAny(title, "技术支持", "售后", "helpdesk")) return "技术支持工程师";
        if (containsAny(title, "硬件测试", "板卡测试", "芯片测试")) return "硬件测试";
        if (containsAny(title, "测试")) return "软件测试";
        if (containsAny(title, "算法", "机器学习", "深度学习", "nlp", "cv")) return "算法工程师";
        if (containsAny(title, "数据分析", "bi", "data analyst")) return "数据分析";
        if (containsAny(title, "前端", "web前端")) return "前端开发";
        if (containsAny(title, "java")) return "Java开发";
        if (containsAny(title, "python")) return "Python开发";
        if (containsAny(title, "运维", "devops", "sre")) return "运维/DevOps";
        if (containsAny(title, "产品经理", "项目经理", "pm")) return "产品/项目经理";
        if (containsAny(title, "运营", "推广", "投放", "增长")) return "APP推广/运营";
        if (containsAny(title, "科研", "研究")) return hasStrongDigitalSignal(text, skills) ? "科研人员" : NON_DIGITAL_JOB;
        if (containsAny(text, "java", "spring", "python", "vue", "react", "mysql", "sql", "docker", "kubernetes", "linux")) {
            return "综合开发";
        }
        return NON_DIGITAL_JOB;
    }

    private boolean hasStrongDigitalSignal(String text, List<String> skills) {
        return (skills != null && !skills.isEmpty())
                || containsAny(text, "软件", "信息化", "数字化", "计算机", "算法", "数据", "java", "python", "前端", "后端", "测试", "运维");
    }

    private List<String> extractSkillsFromText(String text) {
        String source = cleanText(text).toLowerCase(Locale.ROOT);
        List<String> skills = new ArrayList<>();
        for (String keyword : List.of(
                "java", "spring", "spring boot", "spring cloud", "mybatis", "mysql", "redis",
                "python", "django", "flask", "fastapi", "pandas", "numpy",
                "javascript", "typescript", "vue", "react", "node.js", "html", "css",
                "c++", "qt", "linux", "docker", "kubernetes", "git", "sql",
                "oracle", "postgresql", "mongodb", "elasticsearch", "jenkins",
                "selenium", "jmeter", "postman", "tableau", "power bi", "tensorflow", "pytorch",
                "erp", "mes", "wms", "plm", "gis", "arcgis", "plc"
        )) {
            if (source.contains(keyword)) {
                skills.add(normalizeSkill(keyword));
            }
        }
        return skills.stream().distinct().toList();
    }

    private String normalizeSkill(String keyword) {
        return switch (keyword) {
            case "java" -> "Java";
            case "spring boot" -> "Spring Boot";
            case "spring cloud" -> "Spring Cloud";
            case "mybatis" -> "MyBatis";
            case "mysql" -> "MySQL";
            case "redis" -> "Redis";
            case "python" -> "Python";
            case "django" -> "Django";
            case "flask" -> "Flask";
            case "fastapi" -> "FastAPI";
            case "pandas" -> "Pandas";
            case "numpy" -> "NumPy";
            case "javascript" -> "JavaScript";
            case "typescript" -> "TypeScript";
            case "vue" -> "Vue";
            case "react" -> "React";
            case "node.js" -> "Node.js";
            case "html" -> "HTML";
            case "css" -> "CSS";
            case "c++" -> "C++";
            case "qt" -> "Qt";
            case "linux" -> "Linux";
            case "docker" -> "Docker";
            case "kubernetes" -> "Kubernetes";
            case "git" -> "Git";
            case "sql" -> "SQL";
            case "oracle" -> "Oracle";
            case "postgresql" -> "PostgreSQL";
            case "mongodb" -> "MongoDB";
            case "elasticsearch" -> "Elasticsearch";
            case "jenkins" -> "Jenkins";
            case "selenium" -> "Selenium";
            case "jmeter" -> "JMeter";
            case "postman" -> "Postman";
            case "power bi" -> "Power BI";
            case "tableau" -> "Tableau";
            case "tensorflow" -> "TensorFlow";
            case "pytorch" -> "PyTorch";
            case "erp" -> "ERP";
            case "mes" -> "MES";
            case "wms" -> "WMS";
            case "plm" -> "PLM";
            case "gis" -> "GIS";
            case "arcgis" -> "ArcGIS";
            case "plc" -> "PLC";
            default -> keyword;
        };
    }

    private String normalizeCategory(String category) {
        if (blank(category)) {
            return "综合开发";
        }
        if (ALLOWED_CATEGORIES.contains(category)) {
            return category;
        }
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "frontend" -> "前端开发";
            case "backend", "java backend" -> "Java开发";
            case "testing", "qa" -> "软件测试";
            case "data" -> "数据分析";
            case "devops" -> "运维/DevOps";
            case "product manager", "project manager" -> "产品/项目经理";
            case "operations" -> "APP推广/运营";
            case "other" -> "综合开发";
            default -> category;
        };
    }

    private String buildRowHash(Map<String, String> row) {
        return Integer.toHexString(row.toString().hashCode());
    }

    private double clamp(Double value) {
        double safe = value == null ? 0.0 : value;
        return Math.max(0.0, Math.min(1.0, safe));
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!blank(value)) {
                return value;
            }
        }
        return "";
    }

    private String trimLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength);
    }

    private String promptText(String value, int maxLength) {
        return trimLength(cleanText(value), maxLength);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private int readPositiveInt(String propertyName, String envName, int defaultValue) {
        String configured = firstNonBlank(System.getProperty(propertyName), System.getenv(envName));
        if (blank(configured)) {
            return defaultValue;
        }
        try {
            return Math.max(1, Integer.parseInt(configured.trim()));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private double readThreshold(String propertyName, String envName, double defaultValue) {
        String configured = firstNonBlank(System.getProperty(propertyName), System.getenv(envName));
        if (blank(configured)) {
            return defaultValue;
        }
        try {
            return clamp(Double.parseDouble(configured.trim()));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private boolean readBoolean(String propertyName, String envName, boolean defaultValue) {
        String configured = firstNonBlank(System.getProperty(propertyName), System.getenv(envName));
        if (blank(configured)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(configured.trim());
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record RawJobSeed(String sourceId,
                              String sourcePlatform,
                              String companyName,
                              String jobTitle,
                              String industry,
                              String companySize,
                              String companyType,
                              String workAddress,
                              String salaryText,
                              String educationLevel,
                              String experienceText,
                              String jobCode,
                              String jobDescription,
                              String companyIntro,
                              String sourceUrl,
                              String publishedAt,
                              String rowHash) {
    }

    private static final class CleanedPayload {
        private String sourceId = "";
        private String companyName = "";
        private String jobTitle = "";
        private String jobCategory = "";
        private String industry = "";
        private String companySize = "";
        private String companyType = "";
        private String workAddress = "";
        private String city = "";
        private String salaryText = "";
        private String educationLevel = "";
        private String experienceText = "";
        private String jobCode = "";
        private String jobDescription = "";
        private String companyIntro = "";
        private List<String> skills = List.of();
        private List<String> requiredCertificates = List.of();
        private String publishedAt = "";
        private Boolean isValid = Boolean.TRUE;
        private String invalidReason = "";
        private Double confidenceScore = 0.0;
    }
}
