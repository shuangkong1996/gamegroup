package com.gamegroup.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.config.JimengConfig;
import com.gamegroup.dto.TaskResponse;
import com.gamegroup.dto.VideoSubmitRequest;
import com.volcengine.service.visual.impl.VisualServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class JimengVideoService {

    private static final Logger log = LoggerFactory.getLogger(JimengVideoService.class);

    private final VisualServiceImpl visualService;
    private final JimengConfig jimengConfig;

    public JimengVideoService(VisualServiceImpl visualService, JimengConfig jimengConfig) {
        this.visualService = visualService;
        this.jimengConfig = jimengConfig;
    }

    /**
     * 提交视频生成任务
     */
    public TaskResponse submitTask(VideoSubmitRequest request) {
        // 根据建议修改 Action 为异步提交任务
        String action = "CVSync2AsyncSubmitTask";
        String version = "2022-08-31";
        
        Map<String, Object> body = new HashMap<>();
        // req_key 更新为 jimeng_ti2v_v30_pro (即梦图生视频 Pro 版)
//        body.put("req_key", "jimeng_ti2v_v30_pro");
        body.put("req_key", "jimeng_i2v_first_v30_1080");
        // 同时传入 text_prompt 和 prompt 以确保兼容性
        body.put("text_prompt", request.prompt());
        body.put("prompt", request.prompt());
        
        if (request.imageUrl() != null && !request.imageUrl().isEmpty()) {
            body.put("image_urls", Collections.singletonList(request.imageUrl()));
        }
        
        try {
            log.info("Submitting Jimeng task, action={}, version={}, req_key={}, prompt={}", 
                    action, version, body.get("req_key"), request.prompt());
            
            // 使用 visualCommonRequestForJson
            Object responseObj = visualService.visualCommonRequestForJson(body, action, version);
            
            if (responseObj == null) {
                log.error("Jimeng SDK returned null response");
                throw new BusinessException(ErrorCode.JIMENG_VIDEO_SUBMIT_FAILED);
            }
            
            JSONObject response;
            if (responseObj instanceof JSONObject) {
                response = (JSONObject) responseObj;
            } else {
                response = JSON.parseObject(JSON.toJSONString(responseObj));
            }
            
            if (response.containsKey("code") && response.getIntValue("code") != 10000) {
                 String msg = response.getString("message");
                 log.error("Jimeng submit failed: code={}, message={}", response.getIntValue("code"), msg);
                 throw new BusinessException(ErrorCode.JIMENG_VIDEO_SUBMIT_FAILED);
            }
            
            JSONObject dataJson = response.getJSONObject("data");
            if (dataJson == null) {
                log.error("Jimeng response data is null: {}", response);
                throw new BusinessException(ErrorCode.JIMENG_VIDEO_SUBMIT_FAILED);
            }

            String taskId = dataJson.getString("task_id");
            log.info("Jimeng task submitted successfully, taskId={}", taskId);
            
            return new TaskResponse(taskId, "pending", null, null, null);

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Failed to submit Jimeng task", e);
            throw new BusinessException(ErrorCode.JIMENG_VIDEO_SUBMIT_FAILED);
        }
    }

    /**
     * 查询任务状态
     */
    public TaskResponse getTaskStatus(String taskId) {
        // 匹配异步查询接口 CVSync2AsyncGetResult
        String action = "CVSync2AsyncGetResult"; 
        String version = "2022-08-31";
        
        Map<String, Object> body = new HashMap<>();
        body.put("task_id", taskId);
        // 保持一致性，加上 req_key
        body.put("req_key", "jimeng_i2v_first_v30_1080");

        try {
            Object responseObj = visualService.visualCommonRequestForJson(body, action, version);
            
            if (responseObj == null) {
                throw new BusinessException(ErrorCode.JIMENG_VIDEO_QUERY_FAILED);
            }

            JSONObject response;
            if (responseObj instanceof JSONObject) {
                response = (JSONObject) responseObj;
            } else {
                response = JSON.parseObject(JSON.toJSONString(responseObj));
            }
            
            if (response.containsKey("code") && response.getIntValue("code") != 10000) {
                 log.warn("Jimeng query failed: {}", response);
                 throw new BusinessException(ErrorCode.JIMENG_VIDEO_QUERY_FAILED);
            }
            
            JSONObject dataJson = response.getJSONObject("data");
            // DEBUG Log for status check
            log.info("Jimeng query result: {}", dataJson.toJSONString());

            if (dataJson == null) {
                throw new BusinessException(ErrorCode.JIMENG_VIDEO_QUERY_FAILED);
            }
            
            String status = dataJson.getString("status"); 
            String videoUrl = null;
            String coverUrl = null;
            String reason = null;
            
            // 增加对 "Done" 状态的支持 (大小写兼容)
            if ("Succeeded".equalsIgnoreCase(status) || "Success".equalsIgnoreCase(status) || "Done".equalsIgnoreCase(status)) {
                // 优先从 data 直接获取
                if (dataJson.containsKey("video_url")) {
                    videoUrl = dataJson.getString("video_url");
                    coverUrl = dataJson.getString("cover_url");
                } 
                // 兼容旧结构 (如果有 resp)
                else {
                    JSONObject resp = dataJson.getJSONObject("resp");
                    if (resp != null) {
                        videoUrl = resp.getString("video_url");
                        coverUrl = resp.getString("cover_url");
                    }
                }
            } else if ("Failed".equalsIgnoreCase(status)) {
                reason = dataJson.getString("reason");
            }
            
            return new TaskResponse(taskId, status, videoUrl, coverUrl, reason);
            
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Failed to get Jimeng task status", e);
            throw new BusinessException(ErrorCode.JIMENG_VIDEO_QUERY_FAILED);
        }
    }
}
