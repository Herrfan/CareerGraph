package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.NormalizedJobRecordEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.NormalizedJobRecordRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EnterpriseJobCatalogQueryService {
    private static final Set<String> FIRST_TIER_CITIES = Set.of("北京", "上海", "广州", "深圳");
    private static final Set<String> SECOND_TIER_CITIES = Set.of(
            "杭州", "南京", "苏州", "武汉", "成都", "西安", "天津", "重庆", "长沙", "郑州", "青岛", "宁波", "厦门",
            "无锡", "东莞", "佛山", "珠海", "福州", "合肥", "济南", "沈阳", "大连"
    );
    private final NormalizedJobRecordRepository normalizedJobRecordRepository;
    private final JobPortraitAssembler jobPortraitAssembler;

    public EnterpriseJobCatalogQueryService(NormalizedJobRecordRepository normalizedJobRecordRepository,
                                            JobPortraitAssembler jobPortraitAssembler) {
        this.normalizedJobRecordRepository = normalizedJobRecordRepository;
        this.jobPortraitAssembler = jobPortraitAssembler;
    }

    public boolean hasImportedJobs() {
        try {
            return normalizedJobRecordRepository.count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public List<JobProfileDTO> getJobs(int skip, int limit) {
        List<JobProfileDTO> jobs = getAllJobs();
        int safeSkip = Math.max(skip, 0);
        int safeLimit = Math.max(limit, 1);
        if (safeSkip >= jobs.size()) {
            return List.of();
        }
        int endIndex = Math.min(jobs.size(), safeSkip + safeLimit);
        return jobs.subList(safeSkip, endIndex);
    }

    public Optional<JobProfileDTO> getJobById(String jobId) {
        return getAllJobs().stream()
                .filter(job -> normalize(job.jobId()).equals(normalize(jobId)))
                .findFirst();
    }

    public List<JobProfileDTO> getAllJobs() {
        try {
            List<NormalizedJobRecordEntity> activeRecords = normalizedJobRecordRepository.findByStatusOrderByIdAsc("active");
            List<NormalizedJobRecordEntity> source = activeRecords.isEmpty()
                    ? normalizedJobRecordRepository.findAllByOrderByIdAsc()
                    : activeRecords;

            List<NormalizedJobRecordEntity> usableRecords = source.stream()
                    .filter(this::isUsableRecord)
                    .toList();
            if (usableRecords.isEmpty()) {
                return List.of();
            }
            return attachRelatedJobIds(buildAggregatedPortraits(usableRecords));
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<JobProfileDTO> searchSimilarJobs(String query, int limit) {
        String normalized = normalize(query);
        return getAllJobs().stream()
                .sorted((left, right) -> Double.compare(similarity(right, normalized), similarity(left, normalized)))
                .limit(Math.max(limit, 1))
                .toList();
    }

    public Map<String, Object> dataSourceStats() {
        try {
            long total = normalizedJobRecordRepository.count();
            long active = normalizedJobRecordRepository.countByStatus("active");
            return Map.of(
                    "total_records", total,
                    "active_records", active,
                    "using_enterprise_source", total > 0,
                    "prefer_active_records", active > 0
            );
        } catch (Exception e) {
            return Map.of(
                    "total_records", 0L,
                    "active_records", 0L,
                    "using_enterprise_source", false,
                    "prefer_active_records", false
            );
        }
    }

    private List<JobProfileDTO> buildAggregatedPortraits(List<NormalizedJobRecordEntity> records) {
        Map<String, List<NormalizedJobRecordEntity>> grouped = new LinkedHashMap<>();
        for (NormalizedJobRecordEntity record : records) {
            grouped.computeIfAbsent(buildAggregateKey(record), key -> new ArrayList<>()).add(record);
        }

        return grouped.values().stream()
                .map(this::toAggregatedPortrait)
                .sorted(Comparator.comparing(JobProfileDTO::category)
                        .thenComparing(job -> job.cityTier() == null ? "" : job.cityTier())
                        .thenComparing(job -> job.salaryBand() == null ? "" : job.salaryBand()))
                .toList();
    }

    private JobProfileDTO toAggregatedPortrait(List<NormalizedJobRecordEntity> records) {
        NormalizedJobRecordEntity representative = records.stream()
                .max(Comparator.comparingDouble(this::representativeScore))
                .orElse(records.get(0));

        String category = blankToDefault(representative.getJobCategory(), inferCategory(representative.getJobTitle()));
        String cityTier = resolveCityTier(records);
        String salaryBand = resolveSalaryBand(records);
        String experienceText = mostCommon(records.stream().map(NormalizedJobRecordEntity::getExperienceText).toList(), "经验要求未标注");
        List<String> topSkills = topValues(records.stream()
                .flatMap(record -> safeList(record.getSkills()).stream())
                .toList(), 8, "通用能力");
        List<String> topCertificates = topValues(records.stream()
                .flatMap(record -> safeList(record.getRequiredCertificates()).stream())
                .filter(value -> !"无".equals(value))
                .toList(), 4, "无");
        if (topCertificates.size() == 1 && "无".equals(topCertificates.get(0))) {
            topCertificates = List.of("无");
        }
        String salaryRange = buildSalaryRangeLabel(salaryBand, records);
        String citySummary = buildCitySummary(records, cityTier);
        String description = buildGenericDescription(category, cityTier, salaryBand, experienceText, topSkills, records);
        String markdown = jobPortraitAssembler.appendPortraitMarkdown(description, category, topSkills, topCertificates, experienceText, description);
        List<String> sourceRecordIds = records.stream()
                .map(NormalizedJobRecordEntity::getSourceId)
                .filter(this::notBlank)
                .distinct()
                .toList();
        double portraitConfidence = average(records.stream().map(NormalizedJobRecordEntity::getConfidenceScore).toList());
        String portraitTitle = buildPortraitTitle(category, cityTier, salaryBand);
        String aggregateId = UUID.nameUUIDFromBytes(
                (portraitTitle + "|" + category + "|" + cityTier + "|" + salaryBand).getBytes(StandardCharsets.UTF_8)
        ).toString();
        String intro = "通用岗位画像，聚合 " + records.size() + " 条原始JD；样本城市：" + citySummary + "；可通过 source_record_ids 回溯原始岗位。";

        return new JobProfileDTO(
                aggregateId,
                portraitTitle,
                "通用岗位画像",
                description,
                markdown,
                category,
                topSkills,
                topCertificates,
                defaultAbilityWeights(category),
                jobPortraitAssembler.buildAbilityPortrait(category, topSkills, topCertificates, experienceText, description),
                jobPortraitAssembler.buildAbilityPriority(category, topSkills, topCertificates, experienceText, description),
                salaryRange,
                citySummary,
                experienceText,
                "岗位画像聚合样本",
                citySummary,
                "计算机信息化行业",
                records.size() + " 条原始JD",
                "聚合画像",
                buildAggregateKey(representative),
                intro,
                "generic_role_portrait",
                cityTier,
                salaryBand,
                records.size(),
                round(portraitConfidence),
                sourceRecordIds,
                List.of()
        );
    }

    private List<JobProfileDTO> attachRelatedJobIds(List<JobProfileDTO> portraits) {
        Map<String, List<String>> relatedIds = new LinkedHashMap<>();
        for (JobProfileDTO portrait : portraits) {
            List<String> related = portraits.stream()
                    .filter(candidate -> !candidate.jobId().equals(portrait.jobId()))
                    .sorted((left, right) -> Double.compare(relationScore(portrait, right), relationScore(portrait, left)))
                    .limit(3)
                    .map(JobProfileDTO::jobId)
                    .toList();
            relatedIds.put(portrait.jobId(), related);
        }

        return portraits.stream()
                .map(job -> copyWithRelatedIds(job, relatedIds.getOrDefault(job.jobId(), List.of())))
                .toList();
    }

    private JobProfileDTO copyWithRelatedIds(JobProfileDTO job, List<String> relatedIds) {
        return new JobProfileDTO(
                job.jobId(),
                job.title(),
                job.department(),
                job.description(),
                job.descriptionMarkdown(),
                job.category(),
                job.requiredSkills(),
                job.requiredCertificates(),
                job.abilityWeights(),
                job.abilityPortrait(),
                job.abilityPriority(),
                job.salaryRange(),
                job.city(),
                job.experienceRequired(),
                job.companyName(),
                job.workAddress(),
                job.industry(),
                job.companySize(),
                job.companyType(),
                job.jobCode(),
                job.companyIntro(),
                job.portraitScope(),
                job.cityTier(),
                job.salaryBand(),
                job.portraitSourceCount(),
                job.portraitConfidence(),
                job.sourceRecordIds(),
                relatedIds
        );
    }

    private double relationScore(JobProfileDTO left, JobProfileDTO right) {
        double score = 0.0;
        if (normalize(left.category()).equals(normalize(right.category()))) {
            score += 0.45;
        }
        if (normalize(left.cityTier()).equals(normalize(right.cityTier()))) {
            score += 0.12;
        }
        if (normalize(left.salaryBand()).equals(normalize(right.salaryBand()))) {
            score += 0.10;
        }
        Set<String> leftSkills = safeList(left.requiredSkills()).stream().map(this::normalize).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> rightSkills = safeList(right.requiredSkills()).stream().map(this::normalize).collect(Collectors.toCollection(LinkedHashSet::new));
        long shared = leftSkills.stream().filter(rightSkills::contains).count();
        score += Math.min(0.33, shared * 0.08);
        return score;
    }

    private String buildAggregateKey(NormalizedJobRecordEntity entity) {
        String category = blankToDefault(entity.getJobCategory(), inferCategory(entity.getJobTitle()));
        String cityTier = resolveCityTier(blankToDefault(entity.getCity(), extractCityFromAddress(entity.getWorkAddress())));
        String salaryBand = resolveSalaryBand(entity.getSalaryMin(), entity.getSalaryMax(), entity.getSalaryText());
        return normalize(category) + "|" + normalize(cityTier) + "|" + normalize(salaryBand);
    }

    private String buildPortraitTitle(String category, String cityTier, String salaryBand) {
        List<String> labels = new ArrayList<>();
        if (notBlank(cityTier) && !"未标注".equals(cityTier)) {
            labels.add(cityTier);
        }
        if (notBlank(salaryBand) && !"未标注".equals(salaryBand)) {
            labels.add(salaryBand);
        }
        if (labels.isEmpty()) {
            return category;
        }
        return category + "（" + String.join(" / ", labels) + "）";
    }

    private String buildGenericDescription(String category,
                                           String cityTier,
                                           String salaryBand,
                                           String experienceText,
                                           List<String> skills,
                                           List<NormalizedJobRecordEntity> records) {
        List<String> topFragments = topValues(records.stream()
                .flatMap(record -> extractDescriptionFragments(record.getJobDescription()).stream())
                .toList(), 3, "");
        String fragmentSummary = topFragments.stream()
                .filter(this::notBlank)
                .collect(Collectors.joining("；"));
        String skillSummary = String.join("、", safeList(skills).stream().limit(6).toList());
        StringBuilder builder = new StringBuilder();
        builder.append("该画像面向").append(category)
                .append("这一类通用岗位，样本集中在").append(blankToDefault(cityTier, "未标注城市层级"))
                .append("，主要薪资分档为").append(blankToDefault(salaryBand, "未标注"))
                .append("，经验要求以").append(blankToDefault(experienceText, "经验要求未标注")).append("为主。");
        if (notBlank(skillSummary)) {
            builder.append("聚合后较稳定的技能信号包括：").append(skillSummary).append("。");
        }
        if (notBlank(fragmentSummary)) {
            builder.append("原始JD中高频职责/要求片段包括：").append(fragmentSummary).append("。");
        } else {
            builder.append("部分原始JD存在职责字段留空，因此该画像同时参考了技能、薪资和学历等剩余字段。");
        }
        return builder.toString();
    }

    private List<String> extractDescriptionFragments(String description) {
        if (!notBlank(description)) {
            return List.of();
        }
        return Arrays.stream(description.split("[。；;\\n]"))
                .map(String::trim)
                .filter(this::notBlank)
                .filter(text -> text.length() >= 4)
                .limit(5)
                .toList();
    }

    private double representativeScore(NormalizedJobRecordEntity entity) {
        return safe(entity.getConfidenceScore()) * 0.7 + safe(entity.getFieldCompletenessScore()) * 0.3;
    }

    private String buildSalaryRangeLabel(String salaryBand, List<NormalizedJobRecordEntity> records) {
        String commonSalary = mostCommon(records.stream().map(NormalizedJobRecordEntity::getSalaryText).toList(), "");
        if (notBlank(commonSalary)) {
            return salaryBand + "（代表样本：" + commonSalary + "）";
        }
        return salaryBand;
    }

    private String buildCitySummary(List<NormalizedJobRecordEntity> records, String cityTier) {
        List<String> topCities = topValues(records.stream()
                .map(record -> blankToDefault(record.getCity(), extractCityFromAddress(record.getWorkAddress())))
                .toList(), 3, cityTier);
        return topCities.isEmpty() ? cityTier : cityTier + "：" + String.join("、", topCities);
    }

    private String resolveCityTier(List<NormalizedJobRecordEntity> records) {
        return mostCommon(records.stream()
                .map(record -> resolveCityTier(blankToDefault(record.getCity(), extractCityFromAddress(record.getWorkAddress()))))
                .toList(), "未标注");
    }

    private String resolveCityTier(String city) {
        String normalizedCity = blankToDefault(city, "").replace("-", "").replace("市", "");
        for (String value : FIRST_TIER_CITIES) {
            if (normalizedCity.contains(value)) {
                return "一线城市";
            }
        }
        for (String value : SECOND_TIER_CITIES) {
            if (normalizedCity.contains(value)) {
                return "二线城市";
            }
        }
        return notBlank(city) ? "其他城市" : "未标注";
    }

    private String resolveSalaryBand(List<NormalizedJobRecordEntity> records) {
        return mostCommon(records.stream()
                .map(record -> resolveSalaryBand(record.getSalaryMin(), record.getSalaryMax(), record.getSalaryText()))
                .toList(), "未标注");
    }

    private String resolveSalaryBand(Integer salaryMin, Integer salaryMax, String salaryText) {
        Integer midpoint = null;
        if (salaryMin != null && salaryMax != null) {
            midpoint = (salaryMin + salaryMax) / 2;
        } else if (salaryMin != null) {
            midpoint = salaryMin;
        } else if (salaryMax != null) {
            midpoint = salaryMax;
        } else if (notBlank(salaryText)) {
            midpoint = parseSalaryMidpoint(salaryText);
        }

        if (midpoint == null || midpoint <= 0) {
            return "未标注";
        }
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
        List<Integer> values = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(万|千|k|K|元)?").matcher(salaryText);
        while (matcher.find()) {
            double numeric = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2) == null ? "" : matcher.group(2).toLowerCase(Locale.ROOT);
            int monthly = switch (unit) {
                case "万" -> (int) Math.round(numeric * 10000);
                case "千", "k" -> (int) Math.round(numeric * 1000);
                case "元", "" -> numeric > 1000 ? (int) Math.round(numeric) : (int) Math.round(numeric * 1000);
                default -> (int) Math.round(numeric);
            };
            values.add(monthly);
            if (values.size() >= 2) {
                break;
            }
        }
        if (values.isEmpty()) {
            return -1;
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        return (values.get(0) + values.get(1)) / 2;
    }

    private Map<String, Double> defaultAbilityWeights(String category) {
        if ("产品/项目经理".equals(category)) {
            return Map.of("basic_requirements", 0.60, "professional_skills", 0.18, "professional_quality", 0.12, "growth_potential", 0.10);
        }
        if ("实施工程师".equals(category) || "技术支持工程师".equals(category)) {
            return Map.of("basic_requirements", 0.58, "professional_skills", 0.22, "professional_quality", 0.10, "growth_potential", 0.10);
        }
        return Map.of("basic_requirements", 0.56, "professional_skills", 0.26, "professional_quality", 0.08, "growth_potential", 0.10);
    }

    private double similarity(JobProfileDTO job, String query) {
        if (query.isBlank()) {
            return 0.0;
        }
        double score = 0.0;
        if (normalize(job.title()).contains(query) || query.contains(normalize(job.title()))) {
            score += 1.0;
        }
        if (normalize(job.category()).contains(query) || query.contains(normalize(job.category()))) {
            score += 0.4;
        }
        if (normalize(job.cityTier()).contains(query) || normalize(job.salaryBand()).contains(query)) {
            score += 0.18;
        }
        for (String skill : safeList(job.requiredSkills())) {
            if (normalize(skill).contains(query) || query.contains(normalize(skill))) {
                score += 0.25;
            }
        }
        return score;
    }

    private String inferCategory(String jobTitle) {
        String title = normalize(jobTitle);
        if (title.contains("frontend") || title.contains("前端")) return "前端开发";
        if (title.contains("java")) return "Java开发";
        if (title.contains("python")) return "Python开发";
        if (title.contains("c++") || title.contains("c/c++") || title.contains("cc++")) return "C/C++开发";
        if (title.contains("运维") || title.contains("devops") || title.contains("sre")) return "运维/DevOps";
        if (title.contains("硬件测试")) return "硬件测试";
        if (title.contains("测试")) return "软件测试";
        if (title.contains("实施")) return "实施工程师";
        if (title.contains("技术支持")) return "技术支持工程师";
        if (title.contains("算法")) return "算法工程师";
        if (title.contains("数据")) return "数据分析";
        if (title.contains("科研") || title.contains("研究")) return "科研人员";
        if (title.contains("产品") || title.contains("项目经理") || title.contains("pm")) return "产品/项目经理";
        if (title.contains("推广") || title.contains("运营")) return "APP推广/运营";
        return "综合开发";
    }

    private String extractCityFromAddress(String workAddress) {
        if (!notBlank(workAddress)) {
            return "";
        }
        String normalized = workAddress.trim();
        int cityIndex = normalized.indexOf("市");
        if (cityIndex > 0) {
            return normalized.substring(0, cityIndex + 1);
        }
        int hyphenIndex = normalized.indexOf("-");
        if (hyphenIndex > 0) {
            return normalized.substring(0, hyphenIndex);
        }
        return normalized;
    }

    private String mostCommon(List<String> values, String fallback) {
        Map<String, Long> counts = values.stream()
                .filter(this::notBlank)
                .collect(Collectors.groupingBy(String::trim, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(fallback);
    }

    private List<String> topValues(List<String> values, int limit, String fallback) {
        Map<String, Long> counts = values.stream()
                .filter(this::notBlank)
                .collect(Collectors.groupingBy(String::trim, LinkedHashMap::new, Collectors.counting()));
        List<String> top = counts.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .map(Map.Entry::getKey)
                .limit(Math.max(limit, 1))
                .toList();
        return top.isEmpty() ? (notBlank(fallback) ? List.of(fallback) : List.of()) : top;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private double average(List<Double> values) {
        return round(values.stream()
                .filter(value -> value != null && value > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0));
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String blankToDefault(String value, String fallback) {
        return notBlank(value) ? value : fallback;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").replace("/", "").replace("+", "");
    }

    private boolean isUsableRecord(NormalizedJobRecordEntity entity) {
        if (entity == null) {
            return false;
        }
        if (!notBlank(entity.getSourceId()) || !notBlank(entity.getJobTitle()) || !notBlank(entity.getCompanyName())) {
            return false;
        }
        String status = blankToDefault(entity.getStatus(), "");
        if ("rejected".equalsIgnoreCase(status)) {
            return false;
        }
        return ComputerCategoryPolicy.isComputerRelatedCategory(blankToDefault(entity.getJobCategory(), inferCategory(entity.getJobTitle())));
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
