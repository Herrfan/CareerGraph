package com.zust.qyf.careeragent.application;

import com.zust.qyf.careeragent.infrastructure.mysql.entity.UserResumeEntity;
import com.zust.qyf.careeragent.infrastructure.mysql.repository.UserResumeRepository;
import org.springframework.stereotype.Service;

import com.zust.qyf.careeragent.domain.dto.ResumeDTO;

@Service
public class ResumeService {
    private final UserResumeRepository repository;

    public ResumeService(UserResumeRepository repository) {
        this.repository = repository;
    }

    /**
     * 存简历画像
     */
    public void saveResumeData(String userId, ResumeDTO dto) {
        UserResumeEntity entity = repository.findByUserId(userId);
        if (entity == null) {
            entity = new UserResumeEntity();
            entity.setUserId(userId);
        }
        entity.setResumeData(dto);
        repository.save(entity);
    }

    /**
     * 读简历画像
     */
    public ResumeDTO getResumeData(String userId) {
        UserResumeEntity entity = repository.findByUserId(userId);
        return entity != null ? entity.getResumeData() : null;
    }
}
