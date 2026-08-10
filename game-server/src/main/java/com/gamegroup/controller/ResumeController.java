package com.gamegroup.controller;

import com.gamegroup.common.response.ApiResponse;
import com.gamegroup.dto.CreateResumeRequest;
import com.gamegroup.dto.ResumeDTO;
import com.gamegroup.dto.UpdateResumeRequest;
import com.gamegroup.service.ResumeService;
import com.gamegroup.service.resume.ResumeHtmlRenderer;
import com.gamegroup.service.resume.ResumePdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "简历")
@RestController
@RequestMapping("/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeHtmlRenderer resumeHtmlRenderer;
    private final ResumePdfService resumePdfService;

    @Operation(summary = "简历列表")
    @GetMapping
    public ApiResponse<List<ResumeDTO>> list(@RequestAttribute("userId") Long userId) {
        return ApiResponse.success(resumeService.listResumes(userId));
    }

    @Operation(summary = "简历详情")
    @GetMapping("/{id}")
    public ApiResponse<ResumeDTO> get(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(resumeService.getResume(userId, id));
    }

    @Operation(summary = "新建简历")
    @PostMapping
    public ApiResponse<ResumeDTO> create(
            @Valid @RequestBody CreateResumeRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(resumeService.createResume(userId, request));
    }

    @Operation(summary = "更新简历")
    @PutMapping("/{id}")
    public ApiResponse<ResumeDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateResumeRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.success(resumeService.updateResume(userId, id, request));
    }

    @Operation(summary = "删除简历")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        resumeService.deleteResume(userId, id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "根据内容生成 PDF（openhtmltopdf）")
    @PostMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(@Valid @RequestBody CreateResumeRequest request) {
        String html = resumeHtmlRenderer.render(request.getTemplateId(), request.getContent());
        byte[] pdf = resumePdfService.htmlToPdf(html);
        String filename = (request.getTitle() == null || request.getTitle().isBlank() ? "resume" : request.getTitle().trim())
                + ".pdf";
        String encoded = UriUtils.encode(filename, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
