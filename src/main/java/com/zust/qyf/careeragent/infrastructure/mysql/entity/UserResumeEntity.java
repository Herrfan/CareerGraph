package com.zust.qyf.careeragent.infrastructure.mysql.entity;

import com.zust.qyf.careeragent.domain.dto.ResumeDTO;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_resume_profile")
public class UserResumeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Convert(converter = ResumeDTOConverter.class)
    @Column(name = "resume_data", columnDefinition = "json")
    private ResumeDTO resumeData;
}