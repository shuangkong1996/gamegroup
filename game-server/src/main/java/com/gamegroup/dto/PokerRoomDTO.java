package com.gamegroup.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PokerRoomDTO {

    private Long id;
    private String roomNo;
    private String name;
    private Long ownerId;
    private Integer tableSize;
    private BigDecimal smallBlind;
    private BigDecimal bigBlind;
    private BigDecimal buyInMin;
    private BigDecimal buyInMax;
    private Integer thinkSeconds;
    private String status;
    private Integer handNo;
    private Integer currentPlayers;
    private List<PokerPlayerDTO> players;
}
