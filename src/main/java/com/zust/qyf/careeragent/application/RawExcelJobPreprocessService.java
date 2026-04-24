package com.zust.qyf.careeragent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Year;
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

@Service
public class RawExcelJobPreprocessService {
    private static final double REVIEW_CONFIDENCE_THRESHOLD = 0.75;

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
    private static final List<String> CERTIFICATE_HEADERS = List.of("certificate", "certificates", "证书", "证书要求");

    private static final Pattern CITY_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,8})(?:市)?(?:[-/路 ]?([\\u4e00-\\u9fa5]{1,12}(?:区|县|镇|新区|开发区)))?");
    private static final Pattern JOB_CODE_PATTERN = Pattern.compile("(CC[A-Z0-9]+|[A-Z]{2,6}\\d{6,})");
    private static final Pattern COMPANY_SIZE_PATTERN = Pattern.compile("(\\d{1,5}-\\d{1,5}人|\\d{1,5}人以上|少于\\d{1,5}人)");
    private static final Pattern EDUCATION_PATTERN = Pattern.compile("(博士|硕士|研究生|本科|大专|专科|中专|高中)(?:及以上|以上|及以下|以下)?");
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile("(\\d+\\s*(?:-|~|到|至)\\s*\\d+\\s*年|\\d+\\s*年以上|应届(?:毕业生)?|实习(?:生)?|无经验|经验不限)");
    private static final Pattern WORK_TIME_PATTERN = Pattern.compile("\\d{1,2}:\\d{2}\\s*(?:-|~|到|至)\\s*\\d{1,2}:\\d{2}");
    private static final Pattern DATE_TIME_PATTERN = Pattern.compile("\\d{4}-\\d{1,2}-\\d{1,2}(?:\\s+\\d{1,2}:\\d{2}:\\d{2})?");
    private static final Pattern THIN_DESCRIPTION_PATTERN = Pattern.compile("^[\\d\\s./-]+(?:元|k|K|万|千)(?:[^\\p{IsHan}A-Za-z]+)?$");
    private static final Pattern EMBEDDED_JOB_CODE_PATTERN = Pattern.compile("\\b[A-Z]{2,6}\\d[A-Z0-9_-]{5,}\\b");
    private static final Pattern SALARY_NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(万|千|k|K|元)?");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern C_LANGUAGE_PATTERN = Pattern.compile("(?i)(?<![a-z0-9])c(?![a-z0-9+])(?:语言|开发|编程)?");

    private static final List<String> COMPANY_INTRO_MARKERS = List.of(
            "公司简介", "企业简介", "公司介绍", "是一家", "成立于", "总部位于", "专注于", "主营业务", "愿景", "使命", "研发团队",
            "核心价值观", "获得", "认证", "专精特新", "市场份额", "合作院校", "知识产权", "专利", "业务覆盖", "行业领先"
    );
    private static final List<String> BENEFIT_KEYWORDS = List.of(
            "五险一金", "周末双休", "双休", "带薪年假", "节日福利", "生日福利", "定期团建", "商业保险",
            "提供住宿", "免费食宿", "全勤", "餐补", "房补", "补贴", "绩效奖金", "试用期", "加班费", "年终奖"
    );
    private static final List<String> SUBSTANTIVE_KEYWORDS = List.of(
            "负责", "参与", "完成", "开发", "测试", "设计", "部署", "维护", "优化", "编写", "执行",
            "实现", "分析", "需求", "项目", "系统", "平台", "接口", "数据库", "前端", "后端", "实施",
            "支持", "算法", "数据", "产品", "运维", "脚本", "报告", "方案"
    );
    private static final List<String> RESEARCH_TECH_KEYWORDS = List.of(
            "软件", "信息化", "数字化", "计算机", "人工智能", "机器学习", "深度学习", "算法", "数据",
            "编程", "开发", "建模", "大模型", "llm", "rag", "python", "java", "c++", "前端", "后端",
            "测试", "运维", "数据库", "gis", "arcgis", "ngs", "微电子", "集成电路", "半导体", "芯片",
            "电路", "eda", "光刻", "微系统", "封装", "通信", "网络设备", "系统开发"
    );
    private static final List<String> NON_DIGITAL_RESEARCH_KEYWORDS = List.of(
            "植物", "农学", "农业", "生物", "化学", "药学", "药物", "医学", "分子", "细胞",
            "新材料", "混凝土", "土木", "建筑", "畜牧", "兽医", "生态", "作物"
    );
    private static final List<String> ROLE_EVIDENCE_KEYWORDS = List.of(
            "软件", "系统", "平台", "数据库", "mysql", "oracle", "sql", "linux", "windows", "服务器",
            "云平台", "部署", "上线", "接口", "前端", "后端", "开发", "测试", "自动化", "运维",
            "网络", "脚本", "gis", "arcgis", "erp", "mes", "wms", "plm", "python", "java", "c++",
            "计算机软件", "it服务", "信息技术", "通信", "网络设备", "集成电路", "微电子", "半导体"
    );
    private static final List<String> RESPONSIBILITY_HINTS = List.of(
            "负责", "职责", "要求", "熟悉", "掌握", "开发", "测试", "部署", "实施", "支持", "维护", "设计", "经验", "学历"
    );
    private static final List<String> PROMOTIONAL_PHRASES = List.of(
            "提高生产效率", "降低生产成本", "行业领先", "市场份额", "合作院校", "专利", "研发投入", "企业形象", "业务渠道"
    );
    private static final List<String> NON_COMPUTER_INDUSTRY_KEYWORDS = List.of(
            "农/林/牧/渔", "农业", "石油", "化学", "化工", "医药", "建筑", "环保", "电气机械", "电力设备", "水利", "燃气"
    );
    private static final List<String> SUPPORT_EVIDENCE_KEYWORDS = List.of(
            "计算机软件", "it服务", "信息技术", "通信/网络设备", "网络设备", "软件", "系统", "平台",
            "数据库", "服务器", "linux", "windows", "mysql", "oracle", "sql", "云平台", "helpdesk", "系统集成"
    );
    private static final List<String> IMPLEMENTATION_EVIDENCE_KEYWORDS = List.of(
            "计算机软件", "it服务", "软件", "系统", "平台", "部署", "上线", "数据库", "服务器", "云平台",
            "接口", "gis", "arcgis", "erp", "mes", "wms", "plm", "oracle", "mysql", "sql"
    );
    private static final List<String> TEST_EVIDENCE_KEYWORDS = List.of(
            "计算机软件", "软件测试", "测试开发", "自动化测试", "接口测试", "性能测试", "功能测试", "回归测试",
            "selenium", "jmeter", "postman", "硬件测试", "板卡测试", "芯片测试", "示波器", "信号完整性",
            "mcu", "can", "lin", "autosar", "嵌入式", "半导体", "集成电路"
    );
    private static final List<String> COMPUTER_CONTEXT_KEYWORDS = List.of(
            "计算机软件", "it服务", "互联网", "物联网", "人工智能", "通信/网络设备", "网络技术", "软件开发",
            "系统集成", "信息技术", "大数据", "云平台", "半导体", "集成电路", "微电子", "智能硬件"
    );

    private static final List<String> IT_SKILL_KEYWORDS = List.of(
            "Java", "Spring", "Spring Boot", "Spring Cloud", "MyBatis", "MySQL", "Redis", "Kafka",
            "Python", "Django", "Flask", "FastAPI", "Pandas", "NumPy", "JavaScript", "TypeScript",
            "Vue", "React", "Angular", "Node.js", "HTML", "CSS", "Sass", "Webpack", "Vite",
            "C", "C++", "Qt", "Linux", "Docker", "Kubernetes", "Git", "SQL", "Oracle", "PostgreSQL",
            "MongoDB", "Elasticsearch", "RabbitMQ", "Nginx", "JMeter", "Postman", "Selenium",
            "Jenkins", "DevOps", "BI", "Tableau", "Power BI", "TensorFlow", "PyTorch", "OpenCV",
            "ERP", "MES", "WMS", "PLM", "SAP", "ArcGIS", "GIS", "PLC", "嵌入式", "算法", "机器学习"
    );

    private final ObjectMapper objectMapper;

    public RawExcelJobPreprocessService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<NormalizedJobRecord> preprocess(Path inputPath, String sourcePlatform) {
        return preprocess(inputPath, sourcePlatform, null);
    }

    public List<NormalizedJobRecord> preprocess(Path inputPath, String sourcePlatform, Integer maxRows) {
        if (inputPath == null || !Files.exists(inputPath)) {
            return List.of();
        }

        List<NormalizedJobRecord> records = new ArrayList<>();
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
                NormalizedJobRecord record = normalizeRow(values, rowIndex, sourcePlatform);
                if (record != null) {
                    records.add(record);
                    processed++;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to preprocess raw Excel jobs: " + inputPath, e);
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
            throw new IllegalStateException("Failed to export normalized jobs", e);
        }
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

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("total", records.size());
        payload.put("valid", valid.size());
        payload.put("rejected", rejected.size());
        payload.put("review", review.size());
        payload.put("empty_city", records.stream().filter(record -> blank(record.city())).count());
        payload.put("empty_education", records.stream().filter(record -> blank(record.educationLevel())).count());
        payload.put("empty_experience", records.stream().filter(record -> blank(record.experienceText())).count());
        payload.put("top_rejection_reasons", rejectionReasons.entrySet().stream().limit(10).map(entry -> List.of(entry.getKey(), entry.getValue())).toList());
        payload.put("category_distribution", categoryDistribution);
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

    private NormalizedJobRecord normalizeRow(Map<String, String> row, int index, String sourcePlatform) {
        String companyName = firstValue(row, COMPANY_HEADERS);
        String jobTitle = firstValue(row, TITLE_HEADERS);
        if (blank(companyName) && blank(jobTitle)) {
            return null;
        }

        String companyIntro = firstValue(row, COMPANY_INTRO_HEADERS);
        DescriptionBundle descriptionBundle = buildDescriptionBundle(row, companyName, companyIntro);
        String description = sanitizeJobDescription(descriptionBundle.jobDescription());
        String resolvedCompanyIntro = firstNonBlank(companyIntro, descriptionBundle.companyIntro());
        String workAddress = normalizeWorkAddress(firstValue(row, ADDRESS_HEADERS), firstNonBlank(descriptionBundle.locationHint(), description));
        String city = extractCity(workAddress, descriptionBundle.locationHint(), description);
        String salaryText = normalizeSalaryText(firstValue(row, SALARY_HEADERS));
        Integer[] salaryRange = parseSalary(salaryText);
        String industry = firstValue(row, INDUSTRY_HEADERS);
        String companySize = firstNonBlank(firstValue(row, COMPANY_SIZE_HEADERS), descriptionBundle.companySizeHint());
        String companyType = firstValue(row, COMPANY_TYPE_HEADERS);
        String jobCode = extractJobCode(firstNonBlank(firstValue(row, JOB_CODE_HEADERS), descriptionBundle.jobCodeHint()),
                firstValue(row, URL_HEADERS), row.values());
        String educationLevel = normalizeEducation(firstNonBlank(firstValue(row, EDUCATION_HEADERS), descriptionBundle.educationHint(), description));
        String experienceText = normalizeExperience(firstNonBlank(firstValue(row, EXPERIENCE_HEADERS), descriptionBundle.experienceHint(), description));
        List<String> skills = extractSkills(jobTitle, description);
        List<String> certificates = extractCertificates(firstValue(row, CERTIFICATE_HEADERS), description);
        String jobCategory = inferCategory(jobTitle, description, skills);
        boolean digitalJob = !"非信息化岗位".equals(jobCategory)
                && isComputerRole(jobCategory, jobTitle, industry, description, skills);
        if (digitalJob && !hasMeaningfulDescription(description)
                && !hasReliableComputerContext(jobCategory, jobTitle, industry, resolvedCompanyIntro, skills)) {
            jobCategory = "非信息化岗位";
            digitalJob = false;
        }
        String invalidReason = detectInvalidReason(companyName, jobTitle, digitalJob);
        double confidenceScore = confidence(companyName, jobTitle, description, city, salaryRange[0], educationLevel, skills, digitalJob);
        double completenessScore = completeness(companyName, jobTitle, industry, workAddress, city, salaryText, educationLevel, experienceText, description, skills, certificates);
        String sourceId = firstNonBlank(firstValue(row, SOURCE_ID_HEADERS), sourcePlatform + "-" + index);

        return new NormalizedJobRecord(
                sourceId,
                sourcePlatform,
                firstValue(row, URL_HEADERS),
                buildRowHash(companyName, jobTitle, workAddress, description),
                companyName,
                companySize,
                companyType,
                resolvedCompanyIntro,
                jobTitle,
                jobCode,
                jobCategory,
                industry,
                workAddress,
                city,
                salaryText,
                salaryRange[0],
                salaryRange[1],
                educationLevel,
                experienceText,
                description,
                skills,
                certificates,
                firstValue(row, PUBLISHED_AT_HEADERS),
                LocalDateTime.now().toString(),
                invalidReason.isBlank(),
                invalidReason,
                confidenceScore,
                completenessScore,
                "raw_rule_v2"
        );
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

    private String normalizeHeader(String header) {
        return cleanText(header).toLowerCase(Locale.ROOT)
                .replace("：", "")
                .replace(":", "");
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

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("<br>", "；")
                .replace("<br/>", "；")
                .replace("<br />", "；")
                .replace('\u3000', ' ')
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replaceAll("[\\p{Cc}\\p{Cf}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeSalaryText(String salary) {
        return cleanText(salary);
    }

    private Integer[] parseSalary(String salaryText) {
        if (blank(salaryText)) {
            return new Integer[]{null, null};
        }
        String normalized = salaryText.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        String dominantUnit = normalized.contains("万") ? "万"
                : (normalized.contains("k") ? "k"
                : (normalized.contains("千") ? "千" : ""));
        Matcher matcher = SALARY_NUMBER_PATTERN.matcher(normalized);
        List<Integer> numbers = new ArrayList<>();
        while (matcher.find()) {
            double numeric = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2);
            if ((unit == null || unit.isBlank()) && !dominantUnit.isBlank() && numeric < 1000) {
                unit = dominantUnit;
            }
            int salary = switch (unit == null ? "" : unit.toLowerCase(Locale.ROOT)) {
                case "万" -> (int) Math.round(numeric * 10000);
                case "千", "k" -> (int) Math.round(numeric * 1000);
                case "元" -> (int) Math.round(numeric);
                default -> numeric >= 1000 ? (int) Math.round(numeric) : -1;
            };
            if (salary > 0) {
                numbers.add(salary);
            }
        }
        if (numbers.isEmpty()) {
            return new Integer[]{null, null};
        }
        if (numbers.size() == 1) {
            return new Integer[]{numbers.get(0), numbers.get(0)};
        }
        int min = Math.min(numbers.get(0), numbers.get(1));
        int max = Math.max(numbers.get(0), numbers.get(1));
        return new Integer[]{min, max};
    }

    private String normalizeEducation(String education) {
        String cleaned = cleanText(education);
        if (blank(cleaned)) {
            return "";
        }
        Matcher matcher = EDUCATION_PATTERN.matcher(cleaned);
        if (!matcher.find()) {
            return "";
        }
        return switch (matcher.group(1)) {
            case "研究生" -> "硕士";
            case "专科" -> "大专";
            default -> matcher.group(1);
        };
    }

    private String normalizeExperience(String experienceText) {
        String cleaned = cleanText(experienceText);
        if (blank(cleaned)) {
            return "";
        }
        Matcher matcher = EXPERIENCE_PATTERN.matcher(cleaned);
        if (!matcher.find()) {
            return "";
        }
        String value = matcher.group(1).replaceAll("\\s+", "");
        if (value.matches("\\d{4}年")) {
            int year = Integer.parseInt(value.substring(0, 4));
            if (year >= 1900 && year <= Year.now().getValue() + 1) {
                return "";
            }
        }
        return value;
    }

    private String extractJobCode(String fieldValue, String sourceUrl, Iterable<String> rawValues) {
        String direct = cleanText(fieldValue);
        if (!blank(direct)) {
            return direct;
        }
        String url = cleanText(sourceUrl);
        Matcher urlMatcher = JOB_CODE_PATTERN.matcher(url);
        if (urlMatcher.find()) {
            return urlMatcher.group(1);
        }
        for (String rawValue : rawValues) {
            Matcher matcher = JOB_CODE_PATTERN.matcher(cleanText(rawValue));
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    private DescriptionBundle buildDescriptionBundle(Map<String, String> row, String companyName, String companyIntro) {
        String rawDescription = firstValue(row, DESCRIPTION_HEADERS);
        if (blank(rawDescription)) {
            List<String> fallbackFragments = new ArrayList<>();
            for (Map.Entry<String, String> entry : row.entrySet()) {
                String value = cleanText(entry.getValue());
                if (blank(value)) {
                    continue;
                }
                if (COMPANY_HEADERS.contains(entry.getKey())
                        || TITLE_HEADERS.contains(entry.getKey())
                        || COMPANY_INTRO_HEADERS.contains(entry.getKey())
                        || URL_HEADERS.contains(entry.getKey())
                        || SOURCE_ID_HEADERS.contains(entry.getKey())
                        || PUBLISHED_AT_HEADERS.contains(entry.getKey())) {
                    continue;
                }
                if (value.length() >= 8) {
                    fallbackFragments.add(value);
                }
            }
            rawDescription = String.join("；", fallbackFragments);
        }

        String normalized = cleanText(rawDescription);
        String introSeed = cleanText(companyIntro);
        if (blank(normalized)) {
            return new DescriptionBundle("", introSeed, "", "", "", "", "");
        }

        List<String> jobFragments = new ArrayList<>();
        List<String> introFragments = new ArrayList<>();
        String locationHint = "";
        String companySizeHint = "";
        String educationHint = "";
        String experienceHint = "";
        String jobCodeHint = "";

        for (String fragment : normalized.split("[；;。]")) {
            String cleaned = cleanText(fragment);
            if (blank(cleaned)) {
                continue;
            }
            String jobCandidate = stripCompanyIntroTail(cleaned);
            String effective = blank(jobCandidate) ? cleaned : jobCandidate;
            if (blank(locationHint)) {
                locationHint = extractAddressFragment(effective);
            }
            if (blank(companySizeHint)) {
                companySizeHint = extractCompanySizeHint(cleaned);
            }
            if (blank(educationHint) && looksLikeRequirementFragment(effective)) {
                educationHint = normalizeEducation(effective);
            }
            if (blank(experienceHint) && looksLikeRequirementFragment(effective)) {
                experienceHint = normalizeExperience(effective);
            }
            if (blank(jobCodeHint)) {
                jobCodeHint = extractJobCodeHint(cleaned);
            }
            if (looksLikeCompanyIntro(cleaned, companyName, introSeed)) {
                introFragments.add(cleaned);
                continue;
            }
            if (isMetadataFragment(effective)) {
                continue;
            }
            jobFragments.add(effective);
        }

        String resolvedIntro = blank(introSeed) ? String.join("；", introFragments) : introSeed;
        String resolvedDescription = jobFragments.isEmpty()
                ? trimLength(sanitizeJobDescription(normalized), 2400)
                : trimLength(sanitizeJobDescription(String.join("；", jobFragments)), 2400);
        return new DescriptionBundle(
                resolvedDescription,
                trimLength(resolvedIntro, 2400),
                locationHint,
                companySizeHint,
                educationHint,
                experienceHint,
                jobCodeHint
        );
    }

    private String normalizeWorkAddress(String addressField, String description) {
        String direct = cleanText(addressField);
        if (!blank(direct)) {
            direct = direct.split("[；\\n]")[0].trim();
        }
        String candidate = direct.length() <= 80 ? direct : "";
        if (blank(candidate)) {
            candidate = extractAddressFragment(description);
        }
        return trimLength(candidate, 160);
    }

    private String extractAddressFragment(String text) {
        if (blank(text)) {
            return "";
        }
        for (String fragment : text.split("[；\\n]")) {
            String cleaned = cleanText(fragment);
            if (cleaned.length() > 80) {
                continue;
            }
            Matcher matcher = CITY_PATTERN.matcher(cleaned);
            if (matcher.find()) {
                String city = matcher.group(1);
                String district = matcher.group(2);
                return blank(district) ? city : city + "-" + district;
            }
        }
        Matcher matcher = CITY_PATTERN.matcher(text);
        if (matcher.find()) {
            String city = matcher.group(1);
            String district = matcher.group(2);
            return blank(district) ? city : city + "-" + district;
        }
        return "";
    }

    private String extractCity(String... candidates) {
        for (String candidate : candidates) {
            if (blank(candidate)) {
                continue;
            }
            Matcher matcher = CITY_PATTERN.matcher(candidate);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    private List<String> extractSkills(String jobTitle, String description) {
        String haystack = normalizeSkillSource(jobTitle + " " + description);
        List<String> skills = new ArrayList<>();
        for (String keyword : IT_SKILL_KEYWORDS) {
            if (matchesSkillKeyword(haystack, keyword)) {
                skills.add(keyword);
            }
        }
        return skills.stream().distinct().toList();
    }

    private String normalizeSkillSource(String value) {
        String cleaned = cleanText(value).toLowerCase(Locale.ROOT);
        return EMBEDDED_JOB_CODE_PATTERN.matcher(cleaned).replaceAll(" ");
    }

    private boolean matchesSkillKeyword(String source, String keyword) {
        if (blank(source) || blank(keyword)) {
            return false;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        if ("c".equals(normalizedKeyword)) {
            return C_LANGUAGE_PATTERN.matcher(source).find();
        }
        if (normalizedKeyword.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || " ./#+-".indexOf(ch) >= 0)) {
            Pattern pattern = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(normalizedKeyword) + "(?![a-z0-9])");
            return pattern.matcher(source).find();
        }
        return source.contains(normalizedKeyword);
    }

    private List<String> extractCertificates(String certificateText, String description) {
        String combined = cleanText(certificateText + "；" + description);
        List<String> results = new ArrayList<>();
        List<String> candidates = List.of(
                "大学英语四级", "大学英语六级", "计算机二级", "软考", "PMP",
                "华为认证", "思科认证", "法律职业资格", "教师资格证", "证券从业资格",
                "注册会计师", "信息系统项目管理师"
        );
        for (String candidate : candidates) {
            if (combined.contains(candidate)) {
                results.add(candidate);
            }
        }
        return results.isEmpty() ? List.of("无") : results.stream().distinct().toList();
    }

    private String inferCategory(String title, String description, List<String> skills) {
        String normalizedTitle = cleanText(title).toLowerCase(Locale.ROOT);
        String haystack = (cleanText(title) + " " + cleanText(description) + " " + String.join(" ", skills)).toLowerCase(Locale.ROOT);

        if (looksLikeNonTechTitle(normalizedTitle) && !hasStrongTechnicalEvidence(normalizedTitle, haystack, skills)) {
            return "非信息化岗位";
        }

        if (containsAny(normalizedTitle, "实施", "交付")) return hasRoleEvidence(haystack, skills) ? "实施工程师" : "非信息化岗位";
        if (containsAny(normalizedTitle, "技术支持", "售后", "客户支持", "helpdesk")) return hasRoleEvidence(haystack, skills) ? "技术支持工程师" : "非信息化岗位";
        if (containsAny(normalizedTitle, "硬件测试", "板卡测试", "芯片测试")) return hasRoleEvidence(haystack, skills) ? "硬件测试" : "非信息化岗位";
        if (containsAny(normalizedTitle, "测试")) return hasRoleEvidence(haystack, skills) ? "软件测试" : "非信息化岗位";
        if (containsAny(normalizedTitle, "科研", "研究")) return hasResearchRoleEvidence(haystack, skills) ? "科研人员" : "非信息化岗位";
        if (containsAny(normalizedTitle, "算法", "机器学习", "深度学习", "nlp", "cv", "推荐")) return "算法工程师";
        if (containsAny(normalizedTitle, "数据分析", "商业分析", "bi", "data analyst")) return "数据分析";
        if (containsAny(normalizedTitle, "前端", "web前端")) return "前端开发";
        if (containsAny(normalizedTitle, "java")) return "Java开发";
        if (containsAny(normalizedTitle, "python")) return "Python开发";
        if (containsAny(normalizedTitle, "c++", "c/c++", "嵌入式", "单片机")) return "C/C++开发";
        if (containsAny(normalizedTitle, "运维", "devops", "sre")) return "运维/DevOps";
        if (containsAny(normalizedTitle, "产品经理", "项目经理", "项目主管", "pm")) return "产品/项目经理";
        if (containsAny(normalizedTitle, "运营", "推广", "增长", "投放", "渠道")) return "APP推广/运营";

        if (containsAny(haystack, "客户现场", "现场实施", "系统部署", "实施交付", "项目实施", "上线支持", "erp", "mes", "wms", "plm", "arcgis", "gis", "oracle")) {
            return "实施工程师";
        }
        if (containsAny(haystack, "技术支持", "售后支持", "客户支持", "故障排查", "helpdesk")) {
            return "技术支持工程师";
        }
        if (containsAny(haystack, "测试工程师", "测试开发", "软件测试", "自动化测试", "接口测试", "性能测试", "功能测试", "回归测试", "selenium", "jmeter", "postman", "qa")) {
            return "软件测试";
        }
        if (containsAny(haystack, "硬件测试", "电路测试", "板卡测试", "芯片测试", "示波器", "信号完整性", "mcu", "can", "lin", "autosar")) {
            return "硬件测试";
        }
        if (containsAny(haystack, "java", "spring", "后端", "微服务", "mybatis")) return "Java开发";
        if (hasFrontendEvidence(normalizedTitle, haystack, skills)) return "前端开发";
        if (containsAny(haystack, "c++", "c/c++", "嵌入式", "qt", "驱动", "单片机")) return "C/C++开发";
        if (containsAny(haystack, "python", "爬虫", "django", "flask", "fastapi")) return "Python开发";
        if (containsAny(haystack, "数据分析", "bi", "tableau", "power bi", "sql分析", "商业分析")) return "数据分析";
        if (containsAny(haystack, "运维", "devops", "sre", "jenkins", "kubernetes", "docker")) return "运维/DevOps";
        if (containsAny(haystack, "产品经理", "项目经理", "需求分析", "scrum", "项目管理")) return "产品/项目经理";
        if (hasOperationsEvidence(normalizedTitle, haystack)) return "APP推广/运营";
        if (containsAny(haystack, "算法", "机器学习", "深度学习", "nlp", "推荐系统", "cv")) return "算法工程师";
        if (containsAny(haystack, "科研", "研究", "博士后", "实验室", "课题")) return hasResearchRoleEvidence(haystack, skills) ? "科研人员" : "非信息化岗位";
        if (hasStrongDigitalSignal(haystack, skills)) return "综合开发";
        return "非信息化岗位";
    }

    private String extractCompanySizeHint(String text) {
        Matcher matcher = COMPANY_SIZE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractJobCodeHint(String text) {
        Matcher matcher = JOB_CODE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private boolean looksLikeCompanyIntro(String fragment, String companyName, String companyIntro) {
        if (fragment.length() < 18) {
            return false;
        }
        if (!blank(companyIntro) && companyIntro.contains(fragment)) {
            return true;
        }
        if (!blank(companyName) && fragment.contains(companyName) && !containsSubstantiveContent(fragment)) {
            return true;
        }
        if (!blank(companyName) && fragment.contains(companyName) && containsAny(fragment, "公司", "成立于", "是一家", "主营")) {
            return true;
        }
        return COMPANY_INTRO_MARKERS.stream().anyMatch(fragment::contains);
    }

    private String stripCompanyIntroTail(String fragment) {
        if (blank(fragment)) {
            return "";
        }
        int cutIndex = -1;
        for (String marker : COMPANY_INTRO_MARKERS) {
            int markerIndex = fragment.indexOf(marker);
            if (markerIndex >= 0 && markerIndex >= 12) {
                cutIndex = cutIndex < 0 ? markerIndex : Math.min(cutIndex, markerIndex);
            }
        }
        String candidate = cutIndex >= 0 ? fragment.substring(0, cutIndex).trim() : fragment;
        return cleanText(candidate);
    }

    private boolean looksLikeRequirementFragment(String fragment) {
        if (blank(fragment)) {
            return false;
        }
        return containsAny(fragment, "要求", "学历", "本科", "硕士", "博士", "大专", "专科", "应届", "实习", "经验", "年以上");
    }

    private boolean isMetadataFragment(String fragment) {
        String cleaned = sanitizeJobDescription(fragment);
        if (containsAny(cleaned, BENEFIT_KEYWORDS.toArray(String[]::new))) {
            return true;
        }
        if (looksLikePureCompanyPromotion(cleaned)) {
            return true;
        }
        if (looksLikePromotionalBusinessDescription(cleaned)) {
            return true;
        }
        if (!hasMeaningfulDescription(cleaned)) {
            return true;
        }
        if (cleaned.length() <= 32 && COMPANY_SIZE_PATTERN.matcher(cleaned).find()) {
            return true;
        }
        if (cleaned.length() <= 32 && JOB_CODE_PATTERN.matcher(cleaned).find()) {
            return true;
        }
        if (cleaned.length() <= 24 && !extractAddressFragment(cleaned).isBlank()) {
            return true;
        }
        return cleaned.length() <= 24 && WORK_TIME_PATTERN.matcher(cleaned).find();
    }

    private String detectInvalidReason(String companyName, String jobTitle, boolean digitalJob) {
        if (blank(companyName)) return "missing_company_name";
        if (blank(jobTitle)) return "missing_job_title";
        if (!digitalJob) return "non_digital_job";
        return "";
    }

    private double confidence(String companyName,
                              String jobTitle,
                              String description,
                              String city,
                              Integer salaryMin,
                              String educationLevel,
                              List<String> skills,
                              boolean digitalJob) {
        double score = 0.0;
        if (!blank(companyName)) score += 0.15;
        if (!blank(jobTitle)) score += 0.15;
        if (description.length() >= 80) score += 0.2;
        if (!blank(city)) score += 0.1;
        if (salaryMin != null) score += 0.1;
        if (!blank(educationLevel)) score += 0.05;
        if (!skills.isEmpty()) score += Math.min(0.2, skills.size() * 0.025);
        if (digitalJob) score += 0.05;
        return Math.round(Math.min(score, 1.0) * 10000.0) / 10000.0;
    }

    private double completeness(String companyName,
                                String jobTitle,
                                String industry,
                                String workAddress,
                                String city,
                                String salaryText,
                                String educationLevel,
                                String experienceText,
                                String description,
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
        if (!blank(description)) filled++;
        if (skills != null && !skills.isEmpty()) filled++;
        if (certificates != null && !certificates.isEmpty() && !(certificates.size() == 1 && "无".equals(certificates.get(0)))) filled++;
        return Math.round((filled * 1.0 / total) * 10000.0) / 10000.0;
    }

    private String buildRowHash(String companyName, String jobTitle, String workAddress, String description) {
        return Integer.toHexString((companyName + "|" + jobTitle + "|" + workAddress + "|" + description).hashCode());
    }

    private boolean hasStrongDigitalSignal(String haystack, List<String> skills) {
        return !skills.isEmpty() || containsAny(haystack,
                "计算机", "软件", "人工智能", "算法", "数据", "java", "python", "c++", "前端", "后端", "测试", "运维", "devops", "机器学习");
    }

    private boolean hasResearchRoleEvidence(String haystack, List<String> skills) {
        boolean technical = !skills.isEmpty() || containsAny(haystack, RESEARCH_TECH_KEYWORDS.toArray(String[]::new));
        boolean nonDigital = containsAny(haystack, NON_DIGITAL_RESEARCH_KEYWORDS.toArray(String[]::new));
        return technical && !nonDigital;
    }

    private boolean hasRoleEvidence(String haystack, List<String> skills) {
        return !skills.isEmpty() || containsAny(haystack, ROLE_EVIDENCE_KEYWORDS.toArray(String[]::new));
    }

    private boolean isComputerRole(String category,
                                   String jobTitle,
                                   String industry,
                                   String description,
                                   List<String> skills) {
        String haystack = cleanText(jobTitle + " " + industry + " " + description).toLowerCase(Locale.ROOT);
        String normalizedTitle = cleanText(jobTitle).toLowerCase(Locale.ROOT);
        boolean technicalEvidence = hasStrongTechnicalEvidence(normalizedTitle, haystack, skills);
        boolean roleEvidence = hasRoleEvidence(haystack, skills);
        boolean researchEvidence = hasResearchRoleEvidence(haystack, skills);
        boolean nonComputerIndustry = containsAny(industry, NON_COMPUTER_INDUSTRY_KEYWORDS.toArray(String[]::new));

        if (!technicalEvidence && !roleEvidence && !researchEvidence) {
            return false;
        }
        if ("技术支持工程师".equals(category)) {
            boolean supportEvidence = containsAny(haystack, SUPPORT_EVIDENCE_KEYWORDS.toArray(String[]::new));
            return supportEvidence && (!nonComputerIndustry || containsAny(haystack, "计算机软件", "it服务", "系统", "平台", "数据库", "服务器"));
        }
        if ("实施工程师".equals(category)) {
            boolean implementationEvidence = containsAny(haystack, IMPLEMENTATION_EVIDENCE_KEYWORDS.toArray(String[]::new));
            return implementationEvidence && (!nonComputerIndustry || containsAny(haystack, "软件", "系统", "平台", "部署", "云平台", "数据库"));
        }
        if ("软件测试".equals(category) || "硬件测试".equals(category)) {
            boolean testingEvidence = containsAny(haystack, TEST_EVIDENCE_KEYWORDS.toArray(String[]::new));
            return testingEvidence || containsAny(haystack, "计算机软件", "软件测试");
        }
        if ("科研人员".equals(category)) {
            return researchEvidence;
        }
        return true;
    }

    private boolean hasReliableComputerContext(String category,
                                               String jobTitle,
                                               String industry,
                                               String companyIntro,
                                               List<String> skills) {
        String context = cleanText(jobTitle + " " + industry + " " + companyIntro).toLowerCase(Locale.ROOT);
        if (!skills.isEmpty()) {
            return true;
        }
        if (containsAny(context, COMPUTER_CONTEXT_KEYWORDS.toArray(String[]::new))) {
            return true;
        }
        return containsAny(category, "Java开发", "前端开发", "Python开发", "C/C++开发", "算法工程师", "数据分析", "运维/DevOps");
    }

    private boolean hasStrongTechnicalEvidence(String normalizedTitle, String haystack, List<String> skills) {
        if (!skills.isEmpty()) {
            return true;
        }
        if (containsAny(normalizedTitle,
                "\u524D\u7AEF", "\u540E\u7AEF", "java", "python", "c++", "c/c++",
                "\u6D4B\u8BD5", "\u5B9E\u65BD", "\u6280\u672F\u652F\u6301", "\u7B97\u6CD5", "\u6570\u636E",
                "devops", "sre", "\u8FD0\u7EF4", "gis", "arcgis")) {
            return true;
        }
        return containsAny(haystack,
                "java", "spring", "python", "django", "flask", "fastapi", "c++", "c/c++",
                "vue", "react", "javascript", "typescript", "html", "css",
                "selenium", "jmeter", "postman", "oracle", "mysql", "sql",
                "erp", "mes", "wms", "plm", "arcgis", "gis", "docker", "kubernetes");
    }

    private boolean looksLikeNonTechTitle(String normalizedTitle) {
        return containsAny(normalizedTitle,
                "\u52A9\u7406", "\u9500\u552E", "\u5F8B\u5E08", "\u987E\u95EE", "\u62DB\u8058", "\u730E\u5934",
                "\u57F9\u8BAD", "\u7EDF\u8BA1", "\u6863\u6848", "\u8D44\u6599", "\u603B\u52A9", "ceo",
                "\u8463\u4E8B\u957F", "bd", "\u7F51\u7EDC\u9500\u552E", "\u50A8\u5907\u7ECF\u7406", "\u8D28\u68C0",
                "\u5E7F\u544A\u9500\u552E", "\u4EBA\u4E8B", "\u884C\u653F", "\u5BA2\u670D", "\u5185\u5BB9\u5BA1\u6838",
                "\u7FFB\u8BD1", "\u7BA1\u57F9\u751F", "\u7BA1\u7406", "\u8D44\u6599\u7BA1\u7406", "\u6863\u6848\u7BA1\u7406",
                "\u9500\u552E\u5DE5\u7A0B\u5E08", "\u7F51\u7EDC\u5BA2\u670D", "\u7535\u8BDD\u9500\u552E", "\u54A8\u8BE2\u987E\u95EE");
    }

    private boolean hasFrontendEvidence(String normalizedTitle, String haystack, List<String> skills) {
        if (containsAny(normalizedTitle, "\u524D\u7AEF", "web\u524D\u7AEF")) {
            return true;
        }
        long frontendSkillCount = skills.stream()
                .map(skill -> skill == null ? "" : skill.toLowerCase(Locale.ROOT))
                .filter(skill -> containsAny(skill, "vue", "react", "javascript", "typescript", "html", "css"))
                .count();
        if (frontendSkillCount >= 1) {
            return true;
        }
        int signalCount = 0;
        for (String keyword : List.of("vue", "react", "javascript", "typescript", "html", "css", "\u524D\u7AEF")) {
            if (haystack.contains(keyword)) {
                signalCount++;
            }
        }
        return signalCount >= 2;
    }

    private boolean hasOperationsEvidence(String normalizedTitle, String haystack) {
        if (containsAny(normalizedTitle, "\u8FD0\u8425", "\u63A8\u5E7F", "\u6295\u653E", "\u589E\u957F", "app")) {
            return true;
        }
        int signalCount = 0;
        for (String keyword : List.of("app", "\u8FD0\u8425", "\u63A8\u5E7F", "\u6295\u653E", "\u6E20\u9053\u8FD0\u8425", "aso", "seo", "\u7528\u6237\u589E\u957F")) {
            if (haystack.contains(keyword)) {
                signalCount++;
            }
        }
        return signalCount >= 2;
    }

    private String sanitizeJobDescription(String description) {
        String cleaned = cleanText(description);
        cleaned = URL_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = JOB_CODE_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = DATE_TIME_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = cleaned.replaceAll("^[\\d\\s./-]+(?:元|K|k|万|千)[；;,，。\\s-]*", "");
        return cleanText(cleaned);
    }

    private boolean hasMeaningfulDescription(String description) {
        String cleaned = cleanText(description);
        if (cleaned.length() < 12) {
            return false;
        }
        String stripped = DATE_TIME_PATTERN.matcher(cleaned).replaceAll(" ")
                .replaceAll("\\d+(?:\\.\\d+)?\\s*(?:万|千|k|K|元|薪)", " ")
                .replaceAll("[；;，,。\\.·\\-_/]+", " ")
                .trim();
        if (stripped.length() < 4) {
            return false;
        }
        if (THIN_DESCRIPTION_PATTERN.matcher(cleaned).matches()) {
            return false;
        }
        if (!containsSubstantiveContent(cleaned) && containsAny(cleaned, BENEFIT_KEYWORDS.toArray(String[]::new))) {
            return false;
        }
        return true;
    }

    private boolean containsSubstantiveContent(String description) {
        return containsAny(description, SUBSTANTIVE_KEYWORDS.toArray(String[]::new));
    }

    private boolean looksLikePureCompanyPromotion(String description) {
        return description.length() >= 18
                && !containsSubstantiveContent(description)
                && containsAny(description, COMPANY_INTRO_MARKERS.toArray(String[]::new));
    }

    private boolean looksLikePromotionalBusinessDescription(String description) {
        return description.length() >= 24
                && (containsAny(description, COMPANY_INTRO_MARKERS.toArray(String[]::new))
                || containsAny(description, PROMOTIONAL_PHRASES.toArray(String[]::new)))
                && !containsAny(description, RESPONSIBILITY_HINTS.toArray(String[]::new));
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

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record DescriptionBundle(String jobDescription,
                                     String companyIntro,
                                     String locationHint,
                                     String companySizeHint,
                                     String educationHint,
                                     String experienceHint,
                                     String jobCodeHint) {
    }
}
