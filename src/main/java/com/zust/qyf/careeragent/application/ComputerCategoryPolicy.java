package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ComputerCategoryPolicy {
    private static final String GENERAL_DEV = normalize("综合开发");
    private static final String PRODUCT_PM = normalize("产品/项目经理");

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            normalize("前端开发"),
            normalize("Java开发"),
            normalize("Python开发"),
            normalize("C/C++开发"),
            normalize("软件测试"),
            normalize("硬件测试"),
            normalize("运维/DevOps"),
            normalize("实施工程师"),
            normalize("技术支持工程师"),
            normalize("数据分析"),
            normalize("算法工程师"),
            normalize("科研人员"),
            PRODUCT_PM,
            GENERAL_DEV
    );

    private static final List<String> HARD_NON_TECH_KEYWORDS = List.of(
            "sales", "marketing", "business", "bd", "hr", "admin", "finance", "accounting", "legal", "customer-service",
            "销售", "商务", "广告", "市场", "渠道", "地推", "电销", "客服", "人事", "行政", "财务", "会计", "法务", "律师", "导购", "店员", "司机"
    );

    private static final List<String> STRONG_TECH_KEYWORDS = List.of(
            "java", "python", "c++", "c/c++", "frontend", "backend", "fullstack", "devops", "sre", "qa", "test", "algorithm", "data",
            "spring", "mybatis", "mysql", "redis", "kafka", "vue", "react", "typescript", "javascript", "html", "css",
            "linux", "docker", "kubernetes", "jenkins", "sql", "tableau", "powerbi", "tensorflow", "pytorch",
            "开发", "工程师", "前端", "后端", "全栈", "算法", "测试", "运维", "架构", "数据", "实施", "技术支持", "产品经理", "项目经理", "技术经理", "技术总监"
    );

    private ComputerCategoryPolicy() {
    }

    public static boolean isComputerRelatedCategory(String category) {
        return ALLOWED_CATEGORIES.contains(normalize(category));
    }

    public static boolean isComputerRelatedJob(JobProfileDTO job) {
        if (job == null) {
            return false;
        }

        String category = normalize(job.category());
        if (!ALLOWED_CATEGORIES.contains(category)) {
            return false;
        }

        String evidence = buildEvidence(job);
        if (containsAny(evidence, HARD_NON_TECH_KEYWORDS)) {
            return false;
        }

        // 综合开发和产品/项目经理分类容易混入非技术岗位，要求额外技术信号。
        if (GENERAL_DEV.equals(category) || PRODUCT_PM.equals(category)) {
            return containsAny(evidence, STRONG_TECH_KEYWORDS);
        }

        return true;
    }

    public static List<JobProfileDTO> filterComputerJobs(List<JobProfileDTO> jobs) {
        return jobs.stream().filter(ComputerCategoryPolicy::isComputerRelatedJob).toList();
    }

    private static String buildEvidence(JobProfileDTO job) {
        return normalize(
                (job.title() == null ? "" : job.title()) + " "
                        + (job.category() == null ? "" : job.category()) + " "
                        + (job.description() == null ? "" : job.description()) + " "
                        + String.join(" ", job.requiredSkills() == null ? List.of() : job.requiredSkills())
        );
    }

    private static boolean containsAny(String value, List<String> keywords) {
        for (String keyword : keywords) {
            if (value.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replace("/", "")
                .replace("+", "")
                .replace("-", "");
    }
}
