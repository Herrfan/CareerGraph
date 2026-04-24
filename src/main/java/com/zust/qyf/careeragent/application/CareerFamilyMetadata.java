package com.zust.qyf.careeragent.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CareerFamilyMetadata {
    private static final Map<String, FamilyDefinition> FAMILIES = buildFamilies();
    private static final Map<String, FamilyDefinition> ALIAS_INDEX = buildAliasIndex(FAMILIES);

    private CareerFamilyMetadata() {
    }

    public static List<FamilyDefinition> allFamilies() {
        return List.copyOf(FAMILIES.values());
    }

    public static Optional<FamilyDefinition> resolve(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ALIAS_INDEX.get(normalize(value)));
    }

    public static String displayNameOf(String value) {
        return resolve(value).map(FamilyDefinition::displayName).orElse(value == null ? "" : value.trim());
    }

    public static String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_/\\\\（）()\\-]+", "")
                .replace("+", "plus")
                .trim();
    }

    private static Map<String, FamilyDefinition> buildFamilies() {
        LinkedHashMap<String, FamilyDefinition> families = new LinkedHashMap<>();

        families.put("java", new FamilyDefinition(
                "java",
                "Java开发",
                "围绕企业级后端研发、业务模块交付、系统稳定性优化和架构演进展开，强调工程规范、持续交付与复杂业务承接能力。",
                List.of("Java", "Java开发", "java", "后端开发"),
                List.of("Java", "Spring Boot", "MySQL", "Redis", "Kafka", "Linux"),
                List.of("无强制证书", "软件设计师", "云平台认证"),
                List.of(
                        new StageDefinition("工程起步期", "助理/初级Java开发工程师", "先把语言基础、数据库与常规业务接口写稳，能在指导下完成需求拆解与代码交付。"),
                        new StageDefinition("业务交付期", "Java开发工程师", "能够独立承担业务模块开发、接口联调、缺陷修复与常规性能优化。"),
                        new StageDefinition("核心攻坚期", "资深Java开发工程师", "开始负责核心链路、复杂性能问题与高并发场景下的工程治理。"),
                        new StageDefinition("架构引领期", "Java架构师/技术负责人", "主导系统演进、架构选型与团队技术规范，推动平台能力沉淀。")
                ),
                List.of("前端开发", "测试工程师", "项目经理/主管")
        ));

        families.put("cpp", new FamilyDefinition(
                "cpp",
                "C/C++开发",
                "聚焦底层能力、性能优化、系统控制与设备侧研发，强调语言功底、系统理解和稳定性攻关。",
                List.of("C/C++开发", "C_C++", "C/C++", "cpp", "c++"),
                List.of("C", "C++", "Linux", "数据结构", "算法", "调试"),
                List.of("无强制证书", "嵌入式认证", "竞赛奖项"),
                List.of(
                        new StageDefinition("底层打底期", "助理/初级C/C++开发工程师", "先把语言基础、指针内存、编译调试和常见模块修改打牢。"),
                        new StageDefinition("模块开发期", "C/C++开发工程师", "可以独立负责模块设计、接口实现、问题定位和常规性能优化。"),
                        new StageDefinition("系统攻坚期", "资深C/C++开发工程师", "承担复杂系统联调、性能瓶颈排查与关键稳定性问题攻坚。"),
                        new StageDefinition("平台演进期", "系统架构师/技术负责人", "主导平台级架构、底层框架和团队研发规范，带动长期技术演进。")
                ),
                List.of("硬件测试", "科研人员", "技术支持工程师")
        ));

        families.put("product", new FamilyDefinition(
                "product",
                "产品专员/助理",
                "以需求理解、方案整理、跨团队协作和产品落地推进为核心，强调业务洞察与沟通组织能力。",
                List.of("产品专员/助理", "产品专员_助理", "产品专员", "产品助理"),
                List.of("需求分析", "原型设计", "流程梳理", "文档输出", "跨部门协同"),
                List.of("无强制证书", "NPDP", "PMP"),
                List.of(
                        new StageDefinition("需求跟进期", "产品助理", "先把需求记录、竞品整理、流程梳理和原型跟进做好，形成稳定执行能力。"),
                        new StageDefinition("方案打磨期", "产品专员", "能够独立推进需求分析、方案整理、验收协同与版本跟踪。"),
                        new StageDefinition("产品主导期", "产品经理", "负责目标拆解、优先级判断、跨团队协调和完整产品方案交付。"),
                        new StageDefinition("业务统筹期", "高级产品经理/产品负责人", "主导产品方向、业务节奏和关键协作机制，推动产品线持续演进。")
                ),
                List.of("项目经理/主管", "前端开发", "实施工程师")
        ));

        families.put("frontend", new FamilyDefinition(
                "frontend",
                "前端开发",
                "面向 Web、多端界面与交互体验建设，强调页面实现质量、工程化能力和用户体验优化。",
                List.of("前端开发", "前端", "frontend"),
                List.of("HTML", "CSS", "JavaScript", "TypeScript", "Vue", "React"),
                List.of("无强制证书", "前端竞赛奖项", "英语等级证书"),
                List.of(
                        new StageDefinition("页面实现期", "助理/初级前端开发工程师", "先把页面还原、基础交互、组件使用和接口联调能力做扎实。"),
                        new StageDefinition("体验交付期", "前端开发工程师", "能够独立负责业务页面、常规组件封装与性能优化。"),
                        new StageDefinition("架构优化期", "资深前端开发工程师", "负责复杂交互、工程化升级、可视化场景和跨端一致性治理。"),
                        new StageDefinition("前端引领期", "前端架构师/技术负责人", "主导前端架构、组件体系、工程规范与跨团队协同机制。")
                ),
                List.of("Java开发", "产品专员/助理", "测试工程师")
        ));

        families.put("implementation", new FamilyDefinition(
                "implementation",
                "实施工程师",
                "偏重项目落地、系统部署、客户现场交付与问题闭环，强调流程推进、系统理解和现场应变能力。",
                List.of("实施工程师", "实施", "交付工程师"),
                List.of("SQL", "系统部署", "需求沟通", "问题排查", "项目交付"),
                List.of("无强制证书", "项目管理证书", "厂商实施认证"),
                List.of(
                        new StageDefinition("现场熟悉期", "助理/初级实施工程师", "先熟悉业务流程、部署步骤、客户现场规范和基础数据处理。"),
                        new StageDefinition("独立交付期", "实施工程师", "可以独立承担部署上线、培训支持、问题闭环和常规客户沟通。"),
                        new StageDefinition("方案统筹期", "高级实施工程师/交付顾问", "开始统筹项目节奏、关键风险和复杂场景下的实施方案。"),
                        new StageDefinition("交付主导期", "实施负责人/交付经理", "主导跨项目交付标准、资源协调和客户关系维护。")
                ),
                List.of("技术支持工程师", "项目经理/主管", "产品专员/助理")
        ));

        families.put("support", new FamilyDefinition(
                "support",
                "技术支持工程师",
                "围绕售前售后技术支撑、问题定位、客户响应和服务保障展开，强调快速诊断与沟通稳定性。",
                List.of("技术支持工程师", "技术支持", "售后支持"),
                List.of("故障排查", "产品支持", "客户沟通", "网络基础", "文档输出"),
                List.of("无强制证书", "厂商技术认证", "网络认证"),
                List.of(
                        new StageDefinition("服务响应期", "助理/初级技术支持工程师", "先把问题登记、基础排查、标准答复和服务流程走顺。"),
                        new StageDefinition("故障诊断期", "技术支持工程师", "能够独立完成常见问题定位、现场支持和客户沟通闭环。"),
                        new StageDefinition("客户保障期", "高级技术支持工程师", "承担复杂问题升级、关键客户保障和跨团队协调。"),
                        new StageDefinition("支撑统筹期", "技术支持负责人/服务经理", "主导服务质量、支撑机制和核心客户的稳定交付。")
                ),
                List.of("实施工程师", "测试工程师", "项目经理/主管")
        ));

        families.put("test", new FamilyDefinition(
                "test",
                "测试工程师",
                "以功能验证、质量把控、缺陷分析和测试流程协作为主，强调质量意识、执行稳定性与问题追踪能力。",
                List.of("测试工程师", "测试", "QA"),
                List.of("测试用例", "缺陷跟踪", "接口测试", "自动化测试", "质量分析"),
                List.of("无强制证书", "软件测试证书", "自动化测试认证"),
                List.of(
                        new StageDefinition("用例执行期", "助理/初级测试工程师", "先把测试用例执行、缺陷记录和基本回归测试做稳定。"),
                        new StageDefinition("质量协同期", "测试工程师", "能够独立编写用例、跟踪缺陷、配合研发完成质量闭环。"),
                        new StageDefinition("质量把关期", "高级测试工程师", "负责测试方案设计、风险预警、自动化建设和质量节奏把控。"),
                        new StageDefinition("体系建设期", "测试负责人/质量经理", "主导质量标准、测试流程和团队质量能力建设。")
                ),
                List.of("软件测试", "Java开发", "前端开发")
        ));

        families.put("hardware-test", new FamilyDefinition(
                "hardware-test",
                "硬件测试",
                "面向板卡、器件、可靠性和环境验证场景，强调实验规范、数据记录和底层问题分析能力。",
                List.of("硬件测试", "硬件验证"),
                List.of("电路基础", "示波器", "可靠性测试", "故障分析", "实验记录"),
                List.of("无强制证书", "硬件认证", "实验室资质"),
                List.of(
                        new StageDefinition("实验执行期", "助理/初级硬件测试工程师", "先把测试规范、实验操作、数据记录和基础仪器使用做熟。"),
                        new StageDefinition("板级验证期", "硬件测试工程师", "可以独立完成板级验证、可靠性测试和常见问题定位。"),
                        new StageDefinition("攻坚验证期", "高级硬件测试工程师", "承担复杂验证方案、失效分析和关键风险排查。"),
                        new StageDefinition("体系沉淀期", "硬件测试负责人/验证经理", "主导验证流程、质量规范和实验能力体系建设。")
                ),
                List.of("C/C++开发", "科研人员", "技术支持工程师")
        ));

        families.put("research", new FamilyDefinition(
                "research",
                "科研人员",
                "围绕课题研究、实验验证、论文项目和技术突破展开，强调研究方法、持续深挖和创新产出。",
                List.of("科研人员", "科研", "研究人员"),
                List.of("研究方法", "实验设计", "Python", "数据分析", "论文输出"),
                List.of("无强制证书", "论文成果", "科研竞赛奖项"),
                List.of(
                        new StageDefinition("课题跟研期", "科研助理", "先熟悉研究方向、实验流程、资料整理与基础数据处理。"),
                        new StageDefinition("课题攻关期", "科研人员", "能够独立承担子课题、实验验证和阶段性成果输出。"),
                        new StageDefinition("方向突破期", "资深科研人员", "负责关键问题攻关、研究设计与高质量成果沉淀。"),
                        new StageDefinition("研究引领期", "研究负责人/实验室骨干", "主导研究方向、项目协同和团队科研能力建设。")
                ),
                List.of("C/C++开发", "Java开发", "项目经理/主管")
        ));

        families.put("software-test", new FamilyDefinition(
                "software-test",
                "软件测试",
                "更强调自动化、质量平台和测试体系能力，在功能测试基础上进一步延伸到持续质量建设。",
                List.of("软件测试", "软件测试工程师"),
                List.of("自动化测试", "接口测试", "性能测试", "缺陷分析", "测试平台"),
                List.of("无强制证书", "自动化测试认证", "质量管理认证"),
                List.of(
                        new StageDefinition("测试起步期", "助理/初级软件测试工程师", "先把功能回归、接口验证和缺陷跟踪做稳定。"),
                        new StageDefinition("自动化建设期", "软件测试工程师", "能够独立编写测试方案，建设基础自动化和接口测试能力。"),
                        new StageDefinition("平台优化期", "高级软件测试工程师", "承担性能测试、平台沉淀、质量分析和复杂问题定位。"),
                        new StageDefinition("质量体系期", "测试架构师/测试负责人", "主导测试平台、质量流程和团队质量能力建设。")
                ),
                List.of("测试工程师", "Java开发", "前端开发")
        ));

        families.put("pm", new FamilyDefinition(
                "pm",
                "项目经理/主管",
                "核心在于项目全周期推进、资源协调、风险控制和团队沟通，强调组织统筹与结果交付能力。",
                List.of("项目经理/主管", "项目经理_主管", "项目经理", "主管"),
                List.of("项目计划", "资源协调", "风险控制", "汇报复盘", "客户沟通"),
                List.of("PMP", "项目管理证书", "中级职称"),
                List.of(
                        new StageDefinition("项目跟进期", "项目专员/项目协调", "先把节点跟踪、会议纪要、任务推进和基础沟通机制跑顺。"),
                        new StageDefinition("项目负责期", "项目经理", "能够独立负责项目计划、跨团队协同、进度和质量控制。"),
                        new StageDefinition("资源统筹期", "高级项目经理", "承担多方资源协调、复杂风险处置和关键交付节奏把控。"),
                        new StageDefinition("组织引领期", "项目总监/交付平台主管", "主导项目组合管理、组织协同和团队能力建设。")
                ),
                List.of("产品专员/助理", "实施工程师", "技术支持工程师")
        ));

        return families;
    }

    private static Map<String, FamilyDefinition> buildAliasIndex(Map<String, FamilyDefinition> families) {
        LinkedHashMap<String, FamilyDefinition> aliases = new LinkedHashMap<>();
        families.values().forEach(family -> {
            aliases.put(normalize(family.displayName()), family);
            aliases.put(normalize(family.canonicalKey()), family);
            family.aliases().forEach(alias -> aliases.put(normalize(alias), family));
        });
        return aliases;
    }

    public record FamilyDefinition(
            String canonicalKey,
            String displayName,
            String overview,
            List<String> aliases,
            List<String> coreSkills,
            List<String> recommendedCertificates,
            List<StageDefinition> stages,
            List<String> relatedFamilies
    ) {
    }

    public record StageDefinition(String phaseName, String roleName, String summary) {
    }
}
