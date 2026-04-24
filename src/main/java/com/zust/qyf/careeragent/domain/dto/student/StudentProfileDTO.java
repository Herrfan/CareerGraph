package com.zust.qyf.careeragent.domain.dto.student;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StudentProfileDTO(
        @JsonProperty("student_id") String studentId,
        @JsonProperty("basic_info") BasicInfoDTO basicInfo,
        List<String> skills,
        List<String> certificates,
        @JsonProperty("soft_abilities") SoftAbilitiesDTO softAbilities,
        @JsonProperty("job_preference") JobPreferenceDTO jobPreference,
        @JsonProperty("ability_descriptions") AbilityDescriptionsDTO abilityDescriptions,
        @JsonProperty("internship_experiences") List<InternshipExperienceDTO> internshipExperiences,
        @JsonProperty("project_experiences") List<ProjectExperienceDTO> projectExperiences
) {
}
