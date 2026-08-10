package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.dto.OutfitRenderTaskItemDTO;
import com.gamegroup.dto.UserProfileAnalysisResult;
import com.gamegroup.dto.UserProfileDTO;
import com.gamegroup.entity.OutfitRenderTask;
import com.gamegroup.repository.OutfitRenderTaskRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutfitRenderAsyncService {

    private final OutfitRenderTaskRepository outfitRenderTaskRepository;
    private final BailianImageRenderService bailianImageRenderService;
    private final MinioService minioService;

    @Async
    @Transactional
    public void generateRenderImageAsync(Long taskId, UserProfileDTO profile, UserProfileAnalysisResult analysis, List<OutfitRenderTaskItemDTO> items) {
        OutfitRenderTask task = outfitRenderTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("异步生成参考图时未找到任务: taskId={}", taskId);
            return;
        }
        try {
            task.setStatus("RENDERING");
            outfitRenderTaskRepository.save(task);
            log.info("开始异步生成参考图: taskId={}, mode={}", task.getId(), task.getMode());
            String remoteImageUrl = bailianImageRenderService.submitRenderTask(task, profile, analysis, items);
            task.setProviderTaskId(extractProviderTaskId(remoteImageUrl));
            task.setResultImageUrl(persistRenderImage(remoteImageUrl));
            task.setStatus("SUCCESS");
            task.setErrorMessage(null);
            outfitRenderTaskRepository.save(task);
            log.info("异步参考图生成完成: taskId={}, imageUrl={}", task.getId(), remoteImageUrl);
        } catch (BusinessException e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            outfitRenderTaskRepository.save(task);
            log.warn("异步参考图生成失败: taskId={}, error={}", taskId, e.getMessage());
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage("参考图生成失败，请稍后再试");
            outfitRenderTaskRepository.save(task);
            log.error("异步参考图生成异常: taskId={}", taskId, e);
        }
    }

    private String persistRenderImage(String remoteImageUrl) {
        if (remoteImageUrl == null || remoteImageUrl.isBlank()) {
            return remoteImageUrl;
        }
        try (java.io.InputStream inputStream = minioService.getStreamFromUrl(remoteImageUrl)) {
            String fileName = minioService.uploadStream(inputStream, "image/jpeg");
            String fileUrl = minioService.getFileUrl(fileName);
            log.info("百炼结果图已转存到 MinIO: remoteUrl={}, fileUrl={}", remoteImageUrl, fileUrl);
            return fileUrl;
        } catch (Exception e) {
            log.warn("百炼结果图转存 MinIO 失败，保留原始 URL: {}", remoteImageUrl, e);
            return remoteImageUrl;
        }
    }

    private String extractProviderTaskId(String providerResponse) {
        if (providerResponse == null || providerResponse.isBlank()) {
            return null;
        }
        if (providerResponse.startsWith("http://") || providerResponse.startsWith("https://")) {
            return null;
        }
        return providerResponse.length() <= 128 ? providerResponse : providerResponse.substring(0, 128);
    }
}
