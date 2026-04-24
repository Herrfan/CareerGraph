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

@Data
@Entity
@Table(name = "student_profile_snapshot")
public class StudentProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true, length = 128)
    private String studentId;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Convert(converter = StudentProfileDTOConverter.class)
    @Column(name = "profile_data", columnDefinition = "json", nullable = false)
    private StudentProfileDTO profileData;
}
