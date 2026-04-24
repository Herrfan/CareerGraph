package com.zust.qyf.careeragent.web;

import com.zust.qyf.careeragent.application.MatchApplicationService;
import com.zust.qyf.careeragent.domain.dto.match.CategoryMatchDTO;
import com.zust.qyf.careeragent.domain.dto.match.JobMatchDTO;
import com.zust.qyf.careeragent.domain.dto.match.MatchRequestDTO;
import com.zust.qyf.careeragent.domain.dto.match.MatchResultDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/match")
public class MatchController {
    private final MatchApplicationService matchApplicationService;

    public MatchController(MatchApplicationService matchApplicationService) {
        this.matchApplicationService = matchApplicationService;
    }

    @PostMapping("/calculate")
    public MatchResultDTO calculate(@RequestBody MatchRequestDTO request) {
        return matchApplicationService.calculateMatch(request);
    }

    @PostMapping("/batch")
    public Map<String, Object> batch(@RequestBody MatchRequestDTO request) {
        List<JobMatchDTO> matches = matchApplicationService.batchMatch(request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("matches", matches);
        return response;
    }

    @PostMapping("/categories")
    public Map<String, Object> categories(@RequestBody MatchRequestDTO request) {
        List<CategoryMatchDTO> categories = matchApplicationService.categoryMatches(request.studentProfile());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("categories", categories);
        return response;
    }

    @GetMapping("/jobs/{category}")
    public Map<String, Object> getCategoryJobs(@PathVariable String category,
                                               @RequestParam(defaultValue = "5") int limit) {
        List<JobMatchDTO> jobs = matchApplicationService.getCategoryJobs(category, limit);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobs", jobs);
        return response;
    }
}
