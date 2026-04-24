package com.zust.qyf.careeragent.infrastructure.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class AbilityWeightsService {
    private final Map<String, Map<String, Integer>> baseWeights;
    private final Map<String, Map<String, Double>> levelCoefficients;

    public AbilityWeightsService(ObjectMapper objectMapper) {
        try (InputStream inputStream = new PathMatchingResourcePatternResolver()
                .getResource("classpath:ability_weights_config.json")
                .getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            this.baseWeights = parseIntMaps(root.path("base_weights"));
            this.levelCoefficients = parseDoubleNestedMaps(root.path("level_coefficients"));
        } catch (IOException e) {
            throw new IllegalStateException("加载能力权重配置失败", e);
        }
    }

    public Map<String, Double> getWeights(String filename, String experienceRequired, String jobTitle) {
        String category = getJobCategory(filename, jobTitle);
        String level = identifyJobLevel(experienceRequired, jobTitle);
        Map<String, Integer> base = baseWeights.getOrDefault(category, baseWeights.getOrDefault("默认", Map.of()));
        Map<String, Double> coefficients = levelCoefficients.getOrDefault(level, levelCoefficients.getOrDefault("mid", Map.of()));
        Map<String, Double> temp = new HashMap<>();
        double total = 0;
        for (Map.Entry<String, Integer> entry : base.entrySet()) {
            double value = entry.getValue() * coefficients.getOrDefault(entry.getKey(), 1.0);
            temp.put(entry.getKey(), value);
            total += value;
        }
        if (total == 0) {
            return Map.of();
        }
        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, Double> entry : temp.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue() / total);
        }
        return normalized;
    }

    private String identifyJobLevel(String experienceRequired, String jobTitle) {
        String exp = experienceRequired == null ? "" : experienceRequired.toLowerCase();
        String title = jobTitle == null ? "" : jobTitle.toLowerCase();
        if (exp.contains("应届") || exp.contains("实习") || exp.contains("0-3") || exp.contains("1-3") || title.contains("初级") || title.contains("助理")) {
            return "junior";
        }
        if (exp.contains("5年") || exp.contains("6年") || exp.contains("8年") || exp.contains("10年") || title.contains("高级") || title.contains("资深") || title.contains("专家")) {
            return "senior";
        }
        return "mid";
    }

    private String getJobCategory(String filename, String jobTitle) {
        String source = ((filename == null ? "" : filename) + " " + (jobTitle == null ? "" : jobTitle)).toLowerCase();
        if (source.contains("c_c++") || source.contains("java")) return "C_C++_Java";
        if (source.contains("前端")) return "前端开发";
        if (source.contains("软件测试")) return "软件测试";
        if (source.contains("硬件测试")) return "硬件测试";
        if (source.contains("测试工程师")) return "测试工程师";
        if (source.contains("实施工程师")) return "实施工程师";
        if (source.contains("技术支持工程师")) return "技术支持工程师";
        if (source.contains("科研人员")) return "科研人员";
        if (source.contains("项目经理") || source.contains("主管")) return "项目经理_主管";
        if (source.contains("app推广") || source.contains("推广")) return "APP推广";
        return "默认";
    }

    private Map<String, Map<String, Integer>> parseIntMaps(JsonNode node) {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            Map<String, Integer> inner = new HashMap<>();
            node.path(field).fields().forEachRemaining(entry -> inner.put(entry.getKey(), entry.getValue().asInt()));
            result.put(field, inner);
        }
        return result;
    }

    private Map<String, Map<String, Double>> parseDoubleNestedMaps(JsonNode node) {
        Map<String, Map<String, Double>> result = new HashMap<>();
        Iterator<String> fields = node.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            Map<String, Double> inner = new HashMap<>();
            node.path(field).path("coefficients").fields().forEachRemaining(entry -> inner.put(entry.getKey(), entry.getValue().asDouble()));
            result.put(field, inner);
        }
        return result;
    }
}
