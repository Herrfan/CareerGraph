package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.domain.dto.student.StudentProfileDTO;
import com.zust.qyf.careeragent.domain.dto.student.StudentProfileSnapshotDTO;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.StudentProfileHistoryEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.entity.StudentProfileEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.StudentProfileHistoryRepository;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentProfilePersistenceService {
    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileHistoryRepository studentProfileHistoryRepository;

    public StudentProfilePersistenceService(StudentProfileRepository studentProfileRepository,
                                           StudentProfileHistoryRepository studentProfileHistoryRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.studentProfileHistoryRepository = studentProfileHistoryRepository;
    }

    public StudentProfileDTO save(StudentProfileDTO profile) {
        StudentProfileEntity entity = studentProfileRepository.findByStudentId(profile.studentId())
                .orElseGet(StudentProfileEntity::new);
        entity.setStudentId(profile.studentId());
        entity.setProfileData(profile);
        studentProfileRepository.save(entity);
        return profile;
    }

    public StudentProfileDTO saveForUser(Long userId, StudentProfileDTO profile) {
        StudentProfileEntity entity = studentProfileRepository.findByUserId(userId)
                .orElseGet(StudentProfileEntity::new);
        entity.setUserId(userId);
        entity.setStudentId(profile.studentId());
        entity.setProfileData(profile);
        studentProfileRepository.save(entity);

        StudentProfileHistoryEntity history = new StudentProfileHistoryEntity();
        history.setSnapshotId(UUID.randomUUID().toString());
        history.setUserId(userId);
        history.setStudentId(profile.studentId());
        history.setProfileData(profile);
        history.setCreatedAt(LocalDateTime.now());
        studentProfileHistoryRepository.save(history);
        return profile;
    }

    public Optional<StudentProfileDTO> findByStudentId(String studentId) {
        return studentProfileRepository.findByStudentId(studentId).map(StudentProfileEntity::getProfileData);
    }

    public Optional<StudentProfileDTO> findByUserId(Long userId) {
        return studentProfileRepository.findByUserId(userId).map(StudentProfileEntity::getProfileData);
    }

    public List<StudentProfileSnapshotDTO> listHistoryByUserId(Long userId) {
        return studentProfileHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(entity -> new StudentProfileSnapshotDTO(
                        entity.getSnapshotId(),
                        entity.getStudentId(),
                        entity.getProfileData(),
                        entity.getCreatedAt()
                ))
                .toList();
    }
}
