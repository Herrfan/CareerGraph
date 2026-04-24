package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.job.JobProfileDTO;
import com.zust.qyf.careeragent.domain.dto.path.CareerPathResponseDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import org.springframework.stereotype.Service;

@Service
public class CareerPathApplicationService {
    private final CareerFamilyPlannerService careerFamilyPlannerService;

    public CareerPathApplicationService(CareerFamilyPlannerService careerFamilyPlannerService) {
        this.careerFamilyPlannerService = careerFamilyPlannerService;
    }

    public CareerPathResponseDTO getCareerPathByTitle(String jobTitle, int maxDepth, int nResults) {
        return getCareerPathByProfile(null, jobTitle, maxDepth, nResults);
    }

    public CareerPathResponseDTO getCareerPathByProfile(StudentProfileDTO studentProfile,
                                                        String jobTitle,
                                                        int maxDepth,
                                                        int nResults) {
        JobProfileDTO targetJob = careerFamilyPlannerService
                .resolveTargetJob(null, jobTitle, studentProfile)
                .orElseThrow(() -> new IllegalArgumentException("job not found: " + jobTitle));
        return careerFamilyPlannerService.buildCareerPath(targetJob, studentProfile);
    }

    public CareerPathResponseDTO generateCareerPath(String jobTitle) {
        return generateCareerPath(jobTitle, null);
    }

    public CareerPathResponseDTO generateCareerPath(String jobTitle, StudentProfileDTO studentProfile) {
        return getCareerPathByProfile(studentProfile, jobTitle, 4, 5);
    }
}
