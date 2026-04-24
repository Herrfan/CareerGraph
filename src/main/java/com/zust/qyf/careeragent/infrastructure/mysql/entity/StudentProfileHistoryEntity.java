package com.zust.qyf.careeragent.infrastructure.mysql.entity;

import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "student_profile_history")
public class StudentProfileHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id", nullable = false, unique = true, length = 64)
    private String snapshotId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "student_id", nullable = false, length = 128)
    private String studentId;

    @Convert(converter = StudentProfileDTOConverter.class)
    @Column(name = "profile_data", columnDefinition = "json", nullable = false)
    private StudentProfileDTO profileData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
