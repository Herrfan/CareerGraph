package com.zust.qyf.careeragent.utils;


import com.zust.qyf.careeragent.exception.BusinessException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class ResumeUtil {
    /**
     * 清洗简历
     * @param file 简历的原文件 (PDF / DOCX)
     * @return 清洗掉无关元素的纯字符串
     */
    public static String cleanFile(MultipartFile file) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            parser.parse(file.getInputStream(), handler, metadata);
            String rawText = handler.toString();
            if (!StringUtils.hasText(rawText)) {
                return "";
            }
            String cleaned = clearString(rawText);

            // 删首尾
            return cleaned.trim();
        } catch (Exception e) {
            throw new RuntimeException("简历解析失败：" + e.getMessage());
        }
    }

    /**
     * 清洗字符串
     * @param rawText 简历中提取的原始字符串
     * @return 清洗空格回车和html的字符串
     */
    private static String clearString(String rawText) {
        String cleaned = rawText;
        // 洗掉空格 回车 HTML
        // 保留 markdown
        cleaned = cleaned.replaceAll("[\\p{Cc}\\p{Cf}]+", "");
        cleaned = cleaned.replaceAll("[ \\t]+", " ");
        cleaned = cleaned.replaceAll("(\\r?\\n)+", "\n");
        cleaned = cleaned.replaceAll("<[^>]+>", "");

        // 最长文本
        int maxLength = 3000;
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }


    /**
     * 文件合法性校验工具
     */
    public static void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "上传的简历文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && !(filename.endsWith(".pdf")
                || filename.endsWith(".docx")
                || filename.endsWith(".md")
                || filename.endsWith(".txt"))) {
            throw new BusinessException(400, "格式错误：仅支持 PDF, Word(.docx), TXT 或 Markdown 格式");
        }
    }
}