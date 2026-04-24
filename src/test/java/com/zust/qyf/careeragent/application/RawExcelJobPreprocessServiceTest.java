package com.zust.qyf.careeragent.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zust.qyf.careeragent.domain.dto.importer.NormalizedJobRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawExcelJobPreprocessServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void preprocessExtractsStructuredFieldsFromDescriptionAndAvoidsIntroPollution() throws IOException {
        Path workbookPath = tempDir.resolve("jobs.xls");
        writeWorkbook(workbookPath, List.of(
                List.of("company_name", "job_title", "job_description", "company_intro"),
                List.of(
                        "广东南方数码科技股份有限公司",
                        "实施工程师",
                        "广州-天河区；500-999人；CC133964100J40788029412；工作内容：在客户现场进行软件系统的安装部署、数据建库和上线支持；岗位要求：本科及以上学历，1-3年经验，熟悉ArcGIS、Oracle、MySQL；广东南方数码科技股份有限公司是一家专业地理信息软件企业，主营数字城市解决方案；",
                        ""
                )
        ));

        RawExcelJobPreprocessService service = new RawExcelJobPreprocessService(new ObjectMapper());
        List<NormalizedJobRecord> records = service.preprocess(workbookPath, "excel_test");

        assertEquals(1, records.size());
        NormalizedJobRecord record = records.get(0);
        assertEquals("广州", record.city());
        assertEquals("500-999人", record.companySize());
        assertEquals("本科", record.educationLevel());
        assertEquals("1-3年", record.experienceText());
        assertEquals("实施工程师", record.jobCategory());
        assertTrue(record.skills().contains("ArcGIS"));
        assertTrue(record.skills().contains("MySQL"));
        assertFalse(record.jobDescription().contains("是一家专业地理信息软件企业"));
        assertTrue(record.companyIntro().contains("主营数字城市解决方案"));
    }

    @Test
    void preprocessDoesNotUseCompanyIntroToForceNonDigitalResearchRoleIntoFrontend() throws IOException {
        Path workbookPath = tempDir.resolve("research.xls");
        writeWorkbook(workbookPath, List.of(
                List.of("company_name", "job_title", "job_description", "company_intro"),
                List.of(
                        "河南二建集团",
                        "科研人员",
                        "郑州-高新区；主要职责：负责混凝土耐久性研究和成果转化；岗位要求：硕士及以上学历；",
                        "公司是一家数字工厂软件企业，前端团队使用 React 和 Vue 开发平台。"
                )
        ));

        RawExcelJobPreprocessService service = new RawExcelJobPreprocessService(new ObjectMapper());
        List<NormalizedJobRecord> records = service.preprocess(workbookPath, "excel_test");

        assertEquals(1, records.size());
        NormalizedJobRecord record = records.get(0);
        assertEquals("郑州", record.city());
        assertEquals("硕士", record.educationLevel());
        assertEquals("非信息化岗位", record.jobCategory());
        assertTrue(record.skills().isEmpty());
        assertFalse(record.skills().contains("React"));
    }

    @Test
    void preprocessRespectsMaxRowsLimit() throws IOException {
        Path workbookPath = tempDir.resolve("limited.xls");
        writeWorkbook(workbookPath, List.of(
                List.of("company_name", "job_title", "job_description"),
                List.of("甲公司", "Java开发", "负责 Java 后端开发，熟悉 Spring Boot 和 MySQL"),
                List.of("乙公司", "前端开发", "负责 Vue 页面开发与联调"),
                List.of("丙公司", "软件测试", "负责接口测试与自动化测试")
        ));

        RawExcelJobPreprocessService service = new RawExcelJobPreprocessService(new ObjectMapper());
        List<NormalizedJobRecord> records = service.preprocess(workbookPath, "excel_test", 2);

        assertEquals(2, records.size());
        assertEquals("甲公司", records.get(0).companyName());
        assertEquals("乙公司", records.get(1).companyName());
    }

    @Test
    void preprocessParsesWanSalaryRangeWithoutDroppingMagnitude() throws IOException {
        Path workbookPath = tempDir.resolve("salary.xls");
        writeWorkbook(workbookPath, List.of(
                List.of("company_name", "job_title", "salary", "job_description"),
                List.of("甲公司", "Java开发", "1-1.3万", "负责 Java 后端开发，熟悉 Spring Boot、MySQL")
        ));

        RawExcelJobPreprocessService service = new RawExcelJobPreprocessService(new ObjectMapper());
        List<NormalizedJobRecord> records = service.preprocess(workbookPath, "excel_test");

        assertEquals(1, records.size());
        assertEquals(10000, records.get(0).salaryMin());
        assertEquals(13000, records.get(0).salaryMax());
    }

    @Test
    void preprocessRejectsNonComputerResearchRoleEvenWithResearchTitle() throws IOException {
        Path workbookPath = tempDir.resolve("non-tech-research.xls");
        writeWorkbook(workbookPath, List.of(
                List.of("company_name", "job_title", "job_description"),
                List.of(
                        "某农业科技公司",
                        "科研人员",
                        "岗位职责：负责植物代谢产物研究、杀菌剂研发和生物活性测试；岗位要求：植物保护、农学相关专业，硕士学历。"
                )
        ));

        RawExcelJobPreprocessService service = new RawExcelJobPreprocessService(new ObjectMapper());
        List<NormalizedJobRecord> records = service.preprocess(workbookPath, "excel_test");

        assertEquals(1, records.size());
        assertEquals("非信息化岗位", records.get(0).jobCategory());
        assertEquals("non_digital_job", records.get(0).invalidReason());
    }

    private void writeWorkbook(Path target, List<List<String>> rows) throws IOException {
        try (Workbook workbook = new HSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(target)) {
            Sheet sheet = workbook.createSheet("jobs");
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                List<String> cells = rows.get(rowIndex);
                for (int cellIndex = 0; cellIndex < cells.size(); cellIndex++) {
                    row.createCell(cellIndex).setCellValue(cells.get(cellIndex));
                }
            }
            workbook.write(outputStream);
        }
    }
}
