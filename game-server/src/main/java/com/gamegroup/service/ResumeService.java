package com.gamegroup.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.CreateResumeRequest;
import com.gamegroup.dto.ResumeDTO;
import com.gamegroup.dto.UpdateResumeRequest;
import com.gamegroup.entity.Resume;
import com.gamegroup.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final String DEFAULT_TEMPLATE = "classic";

    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ResumeDTO> listResumes(Long userId) {
        return resumeRepository.findByUserIdOrderByUpdateTimeDescIdDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeDTO getResume(Long userId, Long id) {
        return toDto(requireOwned(userId, id));
    }

    @Transactional
    public ResumeDTO createResume(Long userId, CreateResumeRequest request) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(request.getTitle().trim());
        resume.setTemplateId(resolveTemplateId(request.getTemplateId()));
        resume.setContentJson(toJson(request.getContent()));
        return toDto(resumeRepository.save(resume));
    }

    @Transactional
    public ResumeDTO updateResume(Long userId, Long id, UpdateResumeRequest request) {
        Resume resume = requireOwned(userId, id);
        resume.setTitle(request.getTitle().trim());
        resume.setTemplateId(resolveTemplateId(request.getTemplateId()));
        resume.setContentJson(toJson(request.getContent()));
        return toDto(resumeRepository.save(resume));
    }

    @Transactional
    public void deleteResume(Long userId, Long id) {
        Resume resume = requireOwned(userId, id);
        resumeRepository.delete(resume);
    }

    private Resume requireOwned(Long userId, Long id) {
        return resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESUME_NOT_FOUND));
    }

    private String resolveTemplateId(String templateId) {
        return StringUtils.hasText(templateId) ? templateId.trim() : DEFAULT_TEMPLATE;
    }

    private String toJson(JsonNode content) {
        try {
            return objectMapper.writeValueAsString(content == null ? objectMapper.createObjectNode() : content);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private ResumeDTO toDto(Resume resume) {
        return ResumeDTO.builder()
                .id(resume.getId())
                .title(resume.getTitle())
                .templateId(resume.getTemplateId())
                .content(parseContent(resume.getContentJson()))
                .createTime(resume.getCreateTime())
                .updateTime(resume.getUpdateTime())
                .build();
    }

    private JsonNode parseContent(String contentJson) {
        try {
            if (!StringUtils.hasText(contentJson)) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(contentJson);
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }
}
