package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.ResumeDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ResumeFallbackParser {
    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d{9})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern SCHOOL_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,}(大学|学院))");
    private static final Pattern DEGREE_PATTERN = Pattern.compile("(博士|硕士|研究生|本科|学士|大专|专科)");
    private static final Pattern CITY_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,8})(?:市)?");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]{2,4}$");

    private static final List<String> HARD_SKILLS = List.of(
            "Java", "Spring", "Spring Boot", "MySQL", "Redis", "Kafka",
            "Python", "C++", "Vue", "React", "TypeScript", "JavaScript",
            "HTML", "CSS", "Node.js", "Linux", "Docker", "Kubernetes",
            "SQL", "Postman", "Selenium", "JMeter", "TensorFlow", "PyTorch"
    );

    private static final List<String> SOFT_SKILLS = List.of(
            "沟通", "协作", "团队", "学习", "执行", "抗压", "责任", "创新", "组织"
    );

    private static final List<String> MAJORS = List.of(
            "软件工程", "计算机科学与技术", "计算机", "信息工程", "网络工程", "电子信息", "人工智能", "自动化", "通信工程", "数据科学"
    );

    public ResumeDTO parse(String cleanText) {
        String text = cleanText == null ? "" : cleanText;
        String name = extractLikelyName(text);
        String phone = firstMatch(PHONE_PATTERN, text);
        String email = firstMatch(EMAIL_PATTERN, text);
        String school = firstMatch(SCHOOL_PATTERN, text);
        String degree = firstMatch(DEGREE_PATTERN, text);
        String city = firstMatch(CITY_PATTERN, text);
        String major = MAJORS.stream().filter(text::contains).findFirst().orElse("");

        List<String> hardSkills = collectContained(text, HARD_SKILLS);
        List<String> softSkills = collectContained(text, SOFT_SKILLS);

        ResumeDTO.BasicInformation basic = new ResumeDTO.BasicInformation(name, phone, email, null, "", city);
        List<ResumeDTO.EducationExperience> educations = school.isBlank() && degree.isBlank() && major.isBlank()
                ? List.of()
                : List.of(new ResumeDTO.EducationExperience(school, major, degree, ""));
        ResumeDTO.Skill skill = new ResumeDTO.Skill(hardSkills, softSkills);
        ResumeDTO.ResumeScore score = buildScore(hardSkills, softSkills, text);

        return new ResumeDTO(
                guessJobs(hardSkills, text),
                basic,
                educations,
                0,
                List.of(),
                skill,
                List.of(),
                score
        );
    }

    private ResumeDTO.ResumeScore buildScore(List<String> hardSkills, List<String> softSkills, String text) {
        int technical = Math.min(100, 38 + hardSkills.size() * 9);
        int certificates = containsAny(text, List.of("证书", "认证", "竞赛", "获奖", "四级", "六级")) ? 68 : 40;
        int innovation = containsAny(text, List.of("创新", "优化", "改进", "设计", "开源")) ? 70 : 52;
        int learning = containsAny(text, List.of("学习", "课程", "成绩", "自学", "研究")) ? 75 : 58;
        int stress = containsAny(text, List.of("抗压", "并行", "多任务", "高强度")) ? 70 : 56;
        int communication = Math.min(100, 48 + softSkills.size() * 9);
        int internship = containsAny(text, List.of("实习", "项目", "公司", "研发", "校园经历")) ? 70 : 46;
        int total = Math.min(100, Math.round((technical + certificates + innovation + learning + stress + communication + internship) / 7.0f));
        String level = total >= 90 ? "S" : total >= 80 ? "A" : total >= 70 ? "B" : total >= 60 ? "C" : "D";

        List<String> advantages = new ArrayList<>();
        if (!hardSkills.isEmpty()) {
            advantages.add("已提取到技术关键词：" + String.join("、", hardSkills.stream().limit(4).toList()));
        }
        if (advantages.isEmpty()) {
            advantages.add("已完成本地规则解析，可继续在前端手动补充关键信息。");
        }

        List<String> disadvantages = new ArrayList<>();
        if (hardSkills.isEmpty()) {
            disadvantages.add("技术栈不够明确");
        }
        if (!containsAny(text, List.of("项目", "实习", "负责", "开发", "测试"))) {
            disadvantages.add("项目或实习经历证据不足");
        }
        if (disadvantages.isEmpty()) {
            disadvantages.add("建议补充量化成果与岗位相关证据");
        }

        return new ResumeDTO.ResumeScore(
                technical,
                certificates,
                innovation,
                learning,
                stress,
                communication,
                internship,
                total,
                level,
                "外部大模型暂时不可用，当前结果由本地规则解析生成，适合先完成基础画像。",
                advantages,
                disadvantages
        );
    }

    private List<String> guessJobs(List<String> hardSkills, String text) {
        List<String> jobs = new ArrayList<>();
        String joined = String.join(" ", hardSkills).toLowerCase() + " " + text.toLowerCase();
        if (containsAny(joined, List.of("java", "spring", "mybatis"))) jobs.add("Java开发");
        if (containsAny(joined, List.of("vue", "react", "javascript", "typescript"))) jobs.add("前端开发");
        if (containsAny(joined, List.of("python", "django", "flask"))) jobs.add("Python开发");
        if (containsAny(joined, List.of("selenium", "postman", "jmeter", "测试"))) jobs.add("软件测试");
        if (containsAny(joined, List.of("docker", "kubernetes", "linux", "devops"))) jobs.add("运维/DevOps");
        if (containsAny(joined, List.of("算法", "机器学习", "tensorflow", "pytorch"))) jobs.add("算法工程师");
        if (jobs.isEmpty()) {
            jobs.add("综合开发");
        }
        return jobs;
    }

    private String firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractLikelyName(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] lines = text.split("[\\r\\n；;\\|]");
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String line = lines[i].trim();
            if (line.isBlank()) {
                continue;
            }
            if (line.contains("简历") || line.contains("求职") || line.contains("手机") || line.contains("电话") || line.contains("邮箱")) {
                continue;
            }
            if (NAME_PATTERN.matcher(line).matches()) {
                return line;
            }
        }
        return "";
    }

    private List<String> collectContained(String text, List<String> candidates) {
        Set<String> values = new LinkedHashSet<>();
        String lower = text.toLowerCase();
        for (String candidate : candidates) {
            if (lower.contains(candidate.toLowerCase())) {
                values.add(candidate);
            }
        }
        return new ArrayList<>(values);
    }

    private boolean containsAny(String text, List<String> candidates) {
        return candidates.stream().anyMatch(text::contains);
    }
}
