package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.admin.AdminJobUpdateRequestDTO;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.NormalizedJobRecordEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.NormalizedJobRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminEnterpriseService {
    private final NormalizedJobRecordRepository normalizedJobRecordRepository;

    public AdminEnterpriseService(NormalizedJobRecordRepository normalizedJobRecordRepository) {
        this.normalizedJobRecordRepository = normalizedJobRecordRepository;
    }

    public List<NormalizedJobRecordEntity> listJobs() {
        return normalizedJobRecordRepository.findAll();
    }

    public NormalizedJobRecordEntity updateJob(String sourceId, AdminJobUpdateRequestDTO request) {
        NormalizedJobRecordEntity entity = normalizedJobRecordRepository.findBySourceId(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("岗位不存在: " + sourceId));

        if (request.companyName() != null) entity.setCompanyName(request.companyName());
        if (request.jobTitle() != null) entity.setJobTitle(request.jobTitle());
        if (request.jobCategory() != null) entity.setJobCategory(request.jobCategory());
        if (request.city() != null) entity.setCity(request.city());
        if (request.industry() != null) entity.setIndustry(request.industry());
        if (request.salaryText() != null) entity.setSalaryText(request.salaryText());
        if (request.experienceText() != null) entity.setExperienceText(request.experienceText());
        if (request.educationLevel() != null) entity.setEducationLevel(request.educationLevel());
        if (request.jobDescription() != null) entity.setJobDescription(request.jobDescription());
        if (request.skills() != null) entity.setSkills(request.skills());
        if (request.status() != null) entity.setStatus(request.status());

        return normalizedJobRecordRepository.save(entity);
    }
}
