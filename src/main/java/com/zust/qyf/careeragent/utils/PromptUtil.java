package com.zust.qyf.careeragent.utils;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class PromptUtil {

    private final ResourceLoader resourceLoader;

    public PromptUtil(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 提取提示词
     * @param fileName xxx.txt
     * @return 提示词文本
     */
    public String getPrompt(String fileName) {
        try {
            // 路径：classpath:prompts/xxx.txt
            String path = "classpath:prompts/" + fileName;
            Resource resource = resourceLoader.getResource(path);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("读取提示词文件失败：" + fileName, e);
        }
    }
}
