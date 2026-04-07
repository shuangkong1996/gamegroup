package com.gamegroup.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToiletSessionDTO {

    private Long sessionId;

    private LocalDateTime startTime;

    private BigDecimal monthlySalary;

    private Long elapsedSeconds;

    private BigDecimal earnedMoney;
}
