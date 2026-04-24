package com.zust.qyf.careeragent.domain.dto.report;

import java.util.List;

public record ProfileScoringDTO(
        double completenessScore,
        double competitivenessScore,
        List<String> completenessBreakdown,
        List<String> competitivenessBreakdown,
        String targetJobTitle
) {
    public static ProfileScoringDTO create(double completeness, double competitiveness, List<String> completenessDetails, List<String> competitivenessDetails, String jobTitle) {
        return new ProfileScoringDTO(completeness, competitiveness, completenessDetails, competitivenessDetails, jobTitle);
    }
}
