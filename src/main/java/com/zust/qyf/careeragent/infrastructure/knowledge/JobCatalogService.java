package com.zust.qyf.careeragent.infrastructure.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.application.CareerFamilyMetadata;
import com.zust.qyf.careeragent.application.JobPortraitAssembler;
import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class JobCatalogService {
    private static final Path DEFAULT_PROCESSED_PORTRAIT_DIR = Path.of("data", "processed", "岗位画像json");
    private static final String DEFAULT_CLASSPATH_PATTERN = "classpath:knowledge/岗位画像/*_结构化画像.json";
    private static final String DEFAULT_EXPERIENCE = "应届到3年";
    private static final String DEFAULT_CERTIFICATE = "无强制证书";
    private static final String DEFAULT_SALARY_BAND = "薪资面议";
    private static final String FIELD_JOB_TITLE = "岗位名称";
    private static final String FIELD_CITY_SALARY = "城市薪资";
    private static final String FIELD_SKILLS = "专业技能";
    private static final String FIELD_CERTIFICATES = "证书要求";

    private static final Set<String> FIRST_TIER_CITIES = Set.of("北京", "上海", "广州", "深圳");
    private static final Set<String> SECOND_TIER_CITIES = Set.of(
            "杭州", "南京", "苏州", "武汉", "成都", "西安", "天津", "重庆", "长沙", "郑州",
            "青岛", "宁波", "厦门", "无锡", "东莞", "佛山", "珠海", "福州", "合肥", "济南",
            "沈阳", "大连"
    );

    private static final Pattern SALARY_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(万|千|k|K|元)");
    private static final Pattern PAREN_PATTERN = Pattern.compile("\\(([^)]+)\\)|（([^）]+)）");

    private static final Set<String> GENERIC_SKILL_LABELS = Set.of(
            "编程语言", "前端框架", "移动开发", "数据库", "缓存技术", "消息队列", "搜索引擎", "大数据生态",
            "微服务组件", "容器与devops", "web服务器", "分布式架构", "网络协议", "jvm原理与调优",
            "并发编程", "数据结构与算法", "测试与质量保障", "设计模式", "云原生架构", "专业技能"
    );

    private final AbilityWeightsService abilityWeightsService;
    private final ObjectMapper objectMapper;
    private final JobPortraitAssembler jobPortraitAssembler;
    private final Path processedPortraitDir;
    private final String classpathPattern;
    private volatile List<JobProfileDTO> cachedJobs;

    @Autowired
    public JobCatalogService(AbilityWeightsService abilityWeightsService,
                             ObjectMapper objectMapper,
                             JobPortraitAssembler jobPortraitAssembler) {
        this(abilityWeightsService, objectMapper, jobPortraitAssembler, DEFAULT_PROCESSED_PORTRAIT_DIR, DEFAULT_CLASSPATH_PATTERN);
    }

    JobCatalogService(AbilityWeightsService abilityWeightsService,
                      ObjectMapper objectMapper,
                      JobPortraitAssembler jobPortraitAssembler,
                      Path processedPortraitDir,
                      String classpathPattern) {
        this.abilityWeightsService = abilityWeightsService;
        this.objectMapper = objectMapper;
        this.jobPortraitAssembler = jobPortraitAssembler;
        this.processedPortraitDir = processedPortraitDir;
        this.classpathPattern = classpathPattern;
    }

    public List<JobProfileDTO> getJobs() {
        if (cachedJobs == null) {
            cachedJobs = loadJobs();
        }
        return cachedJobs;
    }

    public Optional<JobProfileDTO> getJobById(String jobId) {
        return getJobs().stream().filter(job -> job.jobId().equals(jobId)).findFirst();
    }

    public List<JobProfileDTO> searchSimilarJobs(String query, int limit) {
        String normalized = normalize(query);
        return getJobs().stream()
                .sorted(Comparator.comparingDouble((JobProfileDTO job) -> similarity(job, normalized)).reversed())
                .limit(Math.max(limit, 1))
                .toList();
    }

    public long importJobs() {
        cachedJobs = loadJobs();
        return cachedJobs.size();
    }

    private List<JobProfileDTO> loadJobs() {
        List<JobProfileDTO> processed = loadProcessedPortraitJobs();
        return processed.isEmpty() ? loadClasspathPortraitJobs() : processed;
    }

    private List<JobProfileDTO> loadProcessedPortraitJobs() {
        if (processedPortraitDir == null || !Files.isDirectory(processedPortraitDir)) {
            return List.of();
        }
        try (var stream = Files.list(processedPortraitDir)) {
            return stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(this::readProcessedPortrait)
                    .flatMap(Optional::stream)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("加载岗位画像失败", e);
        }
    }

    private Optional<JobProfileDTO> readProcessedPortrait(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            JsonNode root = objectMapper.readTree(inputStream);
            String filename = path.getFileName().toString();
            String rawTitle = firstNonBlank(root.path(FIELD_JOB_TITLE).asText(""),
                    filename.endsWith(".json") ? filename.substring(0, filename.length() - 5) : filename);
            CareerFamilyMetadata.FamilyDefinition family = CareerFamilyMetadata.resolve(rawTitle)
                    .orElseThrow(() -> new IllegalArgumentException("unknown job family: " + rawTitle));

            List<String> cities = extractCities(root.path(FIELD_CITY_SALARY));
            List<String> salarySamples = extractSalarySamples(root.path(FIELD_CITY_SALARY));
            List<String> skills = extractProcessedSkills(root.path(FIELD_SKILLS), family.coreSkills());
            List<String> certificates = extractProcessedCertificates(root.path(FIELD_CERTIFICATES), family.recommendedCertificates());

            String description = family.overview();
            String markdown = jobPortraitAssembler.appendPortraitMarkdown(
                    description,
                    family.displayName(),
                    skills,
                    certificates,
                    DEFAULT_EXPERIENCE,
                    description
            );

            String familyKey = family.canonicalKey();
            return Optional.of(new JobProfileDTO(
                    UUID.nameUUIDFromBytes(("career-family-" + familyKey).getBytes()).toString(),
                    family.displayName(),
                    "11个岗位族画像体系",
                    description,
                    markdown,
                    family.displayName(),
                    skills,
                    certificates,
                    abilityWeightsService.getWeights(filename, DEFAULT_EXPERIENCE, family.displayName()),
                    jobPortraitAssembler.buildAbilityPortrait(family.displayName(), skills, certificates, DEFAULT_EXPERIENCE, description),
                    jobPortraitAssembler.buildAbilityPriority(family.displayName(), skills, certificates, DEFAULT_EXPERIENCE, description),
                    buildRepresentativeSalary(salarySamples),
                    buildCitySummary(cities),
                    DEFAULT_EXPERIENCE,
                    "11个岗位族画像体系",
                    buildCitySummary(cities),
                    "计算机岗位",
                    "统一岗位族画像",
                    "portrait_family",
                    familyKey,
                    description,
                    "portrait_family",
                    resolveCityTier(cities),
                    resolveSalaryBand(salarySamples),
                    Math.max(1, salarySamples.size()),
                    0.99,
                    List.of(path.toString()),
                    List.of()
            ));
        } catch (IOException e) {
            throw new IllegalStateException("读取岗位画像失败: " + path, e);
        }
    }

    private List<JobProfileDTO> loadClasspathPortraitJobs() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(classpathPattern);
            LinkedHashMap<String, JobProfileDTO> jobs = new LinkedHashMap<>();
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    JsonNode root = objectMapper.readTree(inputStream);
                    String filename = resource.getFilename() == null ? "unknown.json" : resource.getFilename();
                    String rawTitle = root.path(FIELD_JOB_TITLE).asText(filename.replace("_结构化画像.json", ""));
                    CareerFamilyMetadata.FamilyDefinition family = CareerFamilyMetadata.resolve(rawTitle).orElse(null);
                    if (family == null) {
                        continue;
                    }
                    String description = family.overview();
                    String key = family.canonicalKey();
                    jobs.putIfAbsent(key, new JobProfileDTO(
                            UUID.nameUUIDFromBytes(("career-family-" + key).getBytes()).toString(),
                            family.displayName(),
                            "11个岗位族画像体系",
                            description,
                            jobPortraitAssembler.appendPortraitMarkdown(
                                    description,
                                    family.displayName(),
                                    family.coreSkills(),
                                    family.recommendedCertificates(),
                                    DEFAULT_EXPERIENCE,
                                    description
                            ),
                            family.displayName(),
                            family.coreSkills(),
                            family.recommendedCertificates(),
                            abilityWeightsService.getWeights(filename, DEFAULT_EXPERIENCE, family.displayName()),
                            jobPortraitAssembler.buildAbilityPortrait(family.displayName(), family.coreSkills(), family.recommendedCertificates(), DEFAULT_EXPERIENCE, description),
                            jobPortraitAssembler.buildAbilityPriority(family.displayName(), family.coreSkills(), family.recommendedCertificates(), DEFAULT_EXPERIENCE, description),
                            "薪资面议",
                            "全国",
                            DEFAULT_EXPERIENCE,
                            "11个岗位族画像体系",
                            "全国",
                            "计算机岗位",
                            "统一岗位族画像",
                            "portrait_family",
                            key,
                            description,
                            "portrait_family",
                            "全国",
                            DEFAULT_SALARY_BAND,
                            1,
                            0.90,
                            List.of(filename),
                            List.of()
                    ));
                }
            }
            return new ArrayList<>(jobs.values());
        } catch (IOException e) {
            throw new IllegalStateException("加载岗位画像失败", e);
        }
    }

    private List<String> extractCities(JsonNode citySalaryNode) {
        if (!citySalaryNode.isObject()) {
            return List.of();
        }
        LinkedHashSet<String> cities = new LinkedHashSet<>();
        citySalaryNode.fieldNames().forEachRemaining(field -> {
            String city = field.split("-", 2)[0].trim();
            if (!city.isBlank()) {
                cities.add(city);
            }
        });
        return new ArrayList<>(cities);
    }

    private List<String> extractSalarySamples(JsonNode citySalaryNode) {
        if (!citySalaryNode.isObject()) {
            return List.of();
        }
        LinkedHashSet<String> salaries = new LinkedHashSet<>();
        citySalaryNode.fields().forEachRemaining(entry -> {
            String salary = entry.getValue().asText("").trim();
            if (!salary.isBlank()) {
                salaries.add(salary);
            }
        });
        return new ArrayList<>(salaries);
    }

    private List<String> extractProcessedSkills(JsonNode skillsNode, List<String> defaultSkills) {
        LinkedHashSet<String> skills = new LinkedHashSet<>(defaultSkills);
        if (skillsNode.isObject()) {
            skillsNode.fieldNames().forEachRemaining(field -> skills.addAll(extractSkillTokens(field)));
        }
        return skills.stream().filter(this::notBlank).limit(8).toList();
    }

    private List<String> extractSkillTokens(String field) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        String normalizedField = field == null ? "" : field.trim();
        if (normalizedField.isBlank()) {
            return List.of();
        }

        String mainLabel = normalizedField.replace('（', '(').replace('）', ')');
        int splitIndex = mainLabel.indexOf('(');
        String primary = splitIndex >= 0 ? mainLabel.substring(0, splitIndex).trim() : mainLabel;
        if (notBlank(primary) && primary.length() <= 18 && !isGenericSkillLabel(primary)) {
            tokens.add(primary);
        }

        Matcher matcher = PAREN_PATTERN.matcher(normalizedField);
        while (matcher.find()) {
            String group = firstNonBlank(matcher.group(1), matcher.group(2));
            for (String token : group.split("[/、,，]")) {
                String cleaned = normalizeSkillToken(token);
                if (notBlank(cleaned)) {
                    tokens.add(cleaned);
                }
            }
        }
        return new ArrayList<>(tokens);
    }

    private String normalizeSkillToken(String token) {
        String cleaned = token == null ? "" : token.trim();
        if (cleaned.isBlank() || cleaned.length() > 24 || isGenericSkillLabel(cleaned)) {
            return "";
        }
        return switch (cleaned.toLowerCase(Locale.ROOT)) {
            case "javascript" -> "JavaScript";
            case "typescript" -> "TypeScript";
            case "java" -> "Java";
            case "python" -> "Python";
            case "go" -> "Go";
            case "sql" -> "SQL";
            case "shell" -> "Shell";
            case "springboot" -> "Spring Boot";
            case "springcloud" -> "Spring Cloud";
            case "redis" -> "Redis";
            case "mysql" -> "MySQL";
            case "oracle" -> "Oracle";
            case "postgresql" -> "PostgreSQL";
            case "react" -> "React";
            case "vue" -> "Vue";
            case "angular" -> "Angular";
            case "docker" -> "Docker";
            case "kubernetes" -> "Kubernetes";
            case "jenkins" -> "Jenkins";
            case "linux" -> "Linux";
            case "git" -> "Git";
            case "rag" -> "RAG";
            case "llm" -> "LLM";
            default -> cleaned;
        };
    }

    private boolean isGenericSkillLabel(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        return GENERIC_SKILL_LABELS.contains(normalized);
    }

    private List<String> extractProcessedCertificates(JsonNode certificatesNode, List<String> fallback) {
        if (!certificatesNode.isObject()) {
            return fallback;
        }
        List<String> certificates = new ArrayList<>();
        certificatesNode.fieldNames().forEachRemaining(field -> {
            String cleaned = field == null ? "" : field.trim();
            if (!cleaned.isBlank() && !"无".equals(cleaned)) {
                certificates.add(cleaned);
            }
        });
        List<String> distinct = certificates.stream().distinct().limit(6).toList();
        return distinct.isEmpty() ? fallback : distinct;
    }

    private String buildCitySummary(List<String> cities) {
        if (cities == null || cities.isEmpty()) {
            return "全国";
        }
        return cities.stream().limit(5).collect(Collectors.joining("、"));
    }

    private String resolveCityTier(List<String> cities) {
        if (cities == null || cities.isEmpty()) {
            return "全国";
        }
        LinkedHashSet<String> tiers = new LinkedHashSet<>();
        if (cities.stream().anyMatch(this::isFirstTierCity)) {
            tiers.add("一线");
        }
        if (cities.stream().anyMatch(this::isSecondTierCity)) {
            tiers.add("二线");
        }
        if (cities.stream().anyMatch(city -> !isFirstTierCity(city) && !isSecondTierCity(city))) {
            tiers.add("三线及其他");
        }
        return tiers.isEmpty() ? "全国" : String.join(" / ", tiers);
    }

    private boolean isFirstTierCity(String city) {
        return FIRST_TIER_CITIES.stream().anyMatch(city::contains);
    }

    private boolean isSecondTierCity(String city) {
        return SECOND_TIER_CITIES.stream().anyMatch(city::contains);
    }

    private String buildRepresentativeSalary(List<String> salarySamples) {
        if (salarySamples == null || salarySamples.isEmpty()) {
            return "薪资面议";
        }
        IntSummaryStatistics statistics = salarySamples.stream()
                .map(this::parseSalaryBounds)
                .filter(bounds -> bounds.length == 2)
                .flatMapToInt(bounds -> java.util.stream.IntStream.of(bounds[0], bounds[1]))
                .summaryStatistics();
        if (statistics.getCount() == 0) {
            return "薪资面议";
        }
        return statistics.getMin() + "-" + statistics.getMax();
    }

    private String resolveSalaryBand(List<String> salarySamples) {
        List<Integer> midpoints = salarySamples.stream()
                .map(this::parseSalaryMidpoint)
                .filter(value -> value > 0)
                .toList();
        if (midpoints.isEmpty()) {
            return DEFAULT_SALARY_BAND;
        }
        int midpoint = (int) Math.round(midpoints.stream().mapToInt(Integer::intValue).average().orElse(0));
        if (midpoint < 5000) return "5K以下";
        if (midpoint < 10000) return "5K-10K";
        if (midpoint < 15000) return "10K-15K";
        if (midpoint < 20000) return "15K-20K";
        return "20K以上";
    }

    private int parseSalaryMidpoint(String salaryText) {
        if (!notBlank(salaryText)) {
            return -1;
        }
        Matcher matcher = SALARY_PATTERN.matcher(salaryText);
        List<Integer> values = new ArrayList<>();
        while (matcher.find()) {
            double numeric = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase(Locale.ROOT);
            int monthly = switch (unit) {
                case "万" -> (int) Math.round(numeric * 10000);
                case "千", "k" -> (int) Math.round(numeric * 1000);
                default -> numeric > 1000 ? (int) Math.round(numeric) : -1;
            };
            if (monthly > 0) {
                values.add(monthly);
            }
            if (values.size() >= 2) {
                break;
            }
        }
        if (values.isEmpty()) {
            return -1;
        }
        return values.size() == 1 ? values.get(0) : (values.get(0) + values.get(1)) / 2;
    }

    private int[] parseSalaryBounds(String salaryText) {
        if (!notBlank(salaryText)) {
            return new int[0];
        }
        Matcher matcher = SALARY_PATTERN.matcher(salaryText);
        List<Integer> values = new ArrayList<>();
        while (matcher.find()) {
            double numeric = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase(Locale.ROOT);
            int monthly = switch (unit) {
                case "万" -> (int) Math.round(numeric * 10000);
                case "千", "k" -> (int) Math.round(numeric * 1000);
                default -> numeric > 1000 ? (int) Math.round(numeric) : -1;
            };
            if (monthly > 0) {
                values.add(monthly);
            }
        }
        if (values.isEmpty()) {
            return new int[0];
        }
        if (values.size() == 1) {
            int value = values.get(0);
            return new int[] { value, value };
        }
        return new int[] {
                values.stream().min(Integer::compareTo).orElse(values.get(0)),
                values.stream().max(Integer::compareTo).orElse(values.get(values.size() - 1))
        };
    }

    private double similarity(JobProfileDTO job, String query) {
        if (query == null || query.isBlank()) {
            return 0.0;
        }
        double score = 0.0;
        if (normalize(job.title()).contains(query) || query.contains(normalize(job.title()))) {
            score += 1.0;
        }
        if (normalize(job.category()).contains(query) || query.contains(normalize(job.category()))) {
            score += 0.5;
        }
        for (String skill : job.requiredSkills()) {
            if (normalize(skill).contains(query) || query.contains(normalize(skill))) {
                score += 0.2;
            }
        }
        return score;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
