package com.gamegroup.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PokerHandHistoryDTO {

    private Long id;
    private Integer handNo;
    private Integer dealerSeat;
    private String boardCards;
    private BigDecimal pot;
    private LocalDateTime endTime;
    private List<PokerHandPlayerDTO> players;
}
