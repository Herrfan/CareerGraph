package com.zust.qyf.careeragent.domain.dto;

import java.util.List;

/**
 * 把简历输入抽象成简历画像
 * @param jobs                  // 匹配岗位大类 (取 Top3)
 * @param basicInformation      // 基本信息
 * @param educationExperience   // 教育
 * @param workYear              // 工作年限
 * @param workExperience        // 工作经历
 * @param skill                 // 技能
 * @param projects              // 项目
 * @param score                 // 简历总结
 */

public record ResumeDTO(
        List<String> jobs,
        BasicInformation basicInformation,
        List<EducationExperience> educationExperience,
        Integer workYear,
        List<WorkExperience> workExperience,
        Skill skill,
        List<Project> projects,
        ResumeScore score
) {
    // 匹配
    // public record Job (
    //         String name,        // 定死为 {APP推广, C_C++, Java.json, 前端开发....}
    //         String level,       // 等级
    //         Double match        // 匹配度 (0.0 ~ 1.0)
    // ) {}

    // 基本信息
    public record BasicInformation (
            String name,        // 姓名
            String phone,       // 电话
            String email,       // 邮箱
            Integer age,        // 年龄
            String sex,         // 性别
            String city         // 所在城市
    ) {}

    // 教育背景
    public record EducationExperience(
            String school,  // 学校
            String major,   // 专业
            String degree,  // 学位
            String period   // 在读时间（2020-2024 / 2024-至今）
    ) {}

    // 工作经历
    public record WorkExperience(
            String company,     // 企业
            String position,    // 职位
            String period,      // 工作时期
            String achievement  // 量化产出
    ) {}

    // 技能
    public record Skill (
            List<String> hardSkills,    // 硬技能：Java.json/Python/React/MySQL...
            List<String> softSkills     // 软技能：沟通/协作/ leadership...
    ) {}

    // 项目
    public record Project (
            String name,                // 项目名
            String role,                // 担任角色
            String description,         // 项目描述
            List<String> techStacks,    // 技术栈
            String highlight            // 项目亮点/难点
    ) {}

    // 简历评分
    public record ResumeScore (
            // 雷达图
            Integer technical,          // 核心技术栈深度、岗位匹配度、项目实践质量
            Integer certificatesScore,  // 证书含金量与相关性
            Integer innovation,         // 竞赛、专利、创新方案、优化成果
            Integer learning,           // 学历、成绩、学习能力、成长速度
            Integer stressManagement,   // 抗压能力、稳定性、多任务处理表现
            Integer communication,      // 协作、表达、组织、领导力
            Integer internship,         // 实习平台、职责深度、量化产出

            // 评分
            Integer total,              // 总分 0~100
            String level,               // S/A/B/C/D
            String comment,             // 评语
            List<String> advantages,    // 优点
            List<String> disadvantages  // 缺点
    ) {}
}
