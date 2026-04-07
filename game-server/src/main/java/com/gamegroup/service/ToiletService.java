package com.gamegroup.service;

import com.gamegroup.common.exception.BusinessException;
import com.gamegroup.common.response.ErrorCode;
import com.gamegroup.dto.StopToiletResponse;
import com.gamegroup.dto.ToiletRecordDTO;
import com.gamegroup.dto.ToiletSessionDTO;
import com.gamegroup.dto.ToiletStatisticsDTO;
import com.gamegroup.entity.ToiletRecord;
import com.gamegroup.entity.ToiletSession;
import com.gamegroup.repository.ToiletRecordRepository;
import com.gamegroup.repository.ToiletSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToiletService {

    private final ToiletSessionRepository sessionRepository;
    private final ToiletRecordRepository recordRepository;

    // 每月工作天数
    private static final int WORK_DAYS_PER_MONTH = 22;
    // 每天工作小时数
    private static final int WORK_HOURS_PER_DAY = 8;
    // 每小时秒数
    private static final int SECONDS_PER_HOUR = 3600;

    /**
     * 开始计时
     */
    @Transactional
    public ToiletSessionDTO startSession(Long userId, BigDecimal monthlySalary) {
        // 检查是否有进行中的会话，如果有则先结束
        sessionRepository.findByUserIdAndStatus(userId, ToiletSession.SessionStatus.RUNNING)
                .ifPresent(existingSession -> {
                    log.warn("User {} has running session, stopping it first", userId);
                    stopSessionInternal(existingSession);
                });

        // 创建新会话
        ToiletSession session = new ToiletSession();
        session.setUserId(userId);
        session.setStartTime(LocalDateTime.now());
        session.setMonthlySalary(monthlySalary);
        session.setStatus(ToiletSession.SessionStatus.RUNNING);

        session = sessionRepository.save(session);
        log.info("Toilet session started: sessionId={}, userId={}, monthlySalary={}", 
                session.getId(), userId, monthlySalary);

        return ToiletSessionDTO.builder()
                .sessionId(session.getId())
                .startTime(session.getStartTime())
                .monthlySalary(session.getMonthlySalary())
                .elapsedSeconds(0L)
                .earnedMoney(BigDecimal.ZERO)
                .build();
    }

    /**
     * 结束计时并保存记录
     */
    @Transactional
    public StopToiletResponse stopSession(Long userId) {
        ToiletSession session = sessionRepository.findByUserIdAndStatus(userId, ToiletSession.SessionStatus.RUNNING)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOILET_SESSION_NOT_FOUND));

        ToiletRecord record = stopSessionInternal(session);
        log.info("Toilet session stopped: sessionId={}, userId={}, duration={}s, earned={}", 
                session.getId(), userId, record.getDurationSeconds(), record.getEarnedMoney());

        return StopToiletResponse.builder()
                .recordId(record.getId())
                .duration(record.getDurationSeconds())
                .earnedMoney(record.getEarnedMoney())
                .build();
    }

    /**
     * 获取当前会话状态
     */
    public ToiletSessionDTO getCurrentSession(Long userId) {
        ToiletSession session = sessionRepository.findByUserIdAndStatus(userId, ToiletSession.SessionStatus.RUNNING)
                .orElse(null);

        if (session == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = Duration.between(session.getStartTime(), now).getSeconds();
        BigDecimal earnedMoney = calculateEarnedMoney(elapsedSeconds, session.getMonthlySalary());

        return ToiletSessionDTO.builder()
                .sessionId(session.getId())
                .startTime(session.getStartTime())
                .monthlySalary(session.getMonthlySalary())
                .elapsedSeconds(elapsedSeconds)
                .earnedMoney(earnedMoney)
                .build();
    }

    /**
     * 获取历史记录
     */
    public Page<ToiletRecordDTO> getHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "startTime"));
        Page<ToiletRecord> records = recordRepository.findByUserIdOrderByStartTimeDesc(userId, pageable);

        return records.map(this::toRecordDTO);
    }

    /**
     * 获取统计数据
     */
    public ToiletStatisticsDTO getStatistics(Long userId) {
        Integer totalDuration = recordRepository.sumDurationSecondsByUserId(userId);
        BigDecimal totalMoney = recordRepository.sumEarnedMoneyByUserId(userId);
        Long recordCount = recordRepository.countByUserId(userId);

        return ToiletStatisticsDTO.builder()
                .totalDuration(totalDuration != null ? totalDuration : 0)
                .totalMoney(totalMoney != null ? totalMoney : BigDecimal.ZERO)
                .recordCount(recordCount != null ? recordCount : 0L)
                .build();
    }

    /**
     * 内部方法：结束会话并保存记录
     */
    private ToiletRecord stopSessionInternal(ToiletSession session) {
        LocalDateTime endTime = LocalDateTime.now();
        long durationSeconds = Duration.between(session.getStartTime(), endTime).getSeconds();
        BigDecimal earnedMoney = calculateEarnedMoney(durationSeconds, session.getMonthlySalary());

        // 创建记录
        ToiletRecord record = new ToiletRecord();
        record.setUserId(session.getUserId());
        record.setStartTime(session.getStartTime());
        record.setEndTime(endTime);
        record.setDurationSeconds((int) durationSeconds);
        record.setEarnedMoney(earnedMoney);
        record.setMonthlySalary(session.getMonthlySalary());

        record = recordRepository.save(record);

        // 更新会话状态
        session.setStatus(ToiletSession.SessionStatus.STOPPED);
        sessionRepository.save(session);

        return record;
    }

    /**
     * 计算赚取的金额
     * 公式：时长(秒) * 月薪 / 22天 / 8小时 / 3600秒
     */
    private BigDecimal calculateEarnedMoney(long durationSeconds, BigDecimal monthlySalary) {
        if (durationSeconds <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal secondsPerMonth = BigDecimal.valueOf(WORK_DAYS_PER_MONTH)
                .multiply(BigDecimal.valueOf(WORK_HOURS_PER_DAY))
                .multiply(BigDecimal.valueOf(SECONDS_PER_HOUR));

        return BigDecimal.valueOf(durationSeconds)
                .multiply(monthlySalary)
                .divide(secondsPerMonth, 2, RoundingMode.HALF_UP);
    }

    /**
     * 转换为 DTO
     */
    private ToiletRecordDTO toRecordDTO(ToiletRecord record) {
        return ToiletRecordDTO.builder()
                .id(record.getId())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .durationSeconds(record.getDurationSeconds())
                .earnedMoney(record.getEarnedMoney())
                .build();
    }
}
