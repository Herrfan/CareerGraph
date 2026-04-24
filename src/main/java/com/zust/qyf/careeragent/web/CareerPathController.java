package com.zust.qyf.careeragent.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zust.qyf.careeragent.application.CareerPathApplicationService;
import com.zust.qyf.careeragent.domain.dto.path.CareerPathResponseDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/retrieve")
public class CareerPathController {
    private final CareerPathApplicationService careerPathApplicationService;

    public CareerPathController(CareerPathApplicationService careerPathApplicationService) {
        this.careerPathApplicationService = careerPathApplicationService;
    }

    @GetMapping("/career-path/{jobTitle}")
    public CareerPathResponseDTO getCareerPath(@PathVariable String jobTitle,
                                               @RequestParam(name = "max_depth", defaultValue = "3") int maxDepth,
                                               @RequestParam(name = "n_results", defaultValue = "5") int nResults) {
        return careerPathApplicationService.getCareerPathByTitle(jobTitle, maxDepth, nResults);
    }

    @PostMapping("/career-path/generate")
    public CareerPathResponseDTO generateCareerPath(@RequestBody GenerateCareerPathRequest request) {
        return careerPathApplicationService.generateCareerPath(request.jobTitle(), request.studentProfile());
    }

    public record GenerateCareerPathRequest(@JsonProperty("job_title") @JsonAlias("jobTitle") String jobTitle,
                                            @JsonProperty("student_profile") @JsonAlias("studentProfile") StudentProfileDTO studentProfile) {
    }
}
