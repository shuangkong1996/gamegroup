package com.gamegroup.poker.ws;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户端 → 服务端的 WebSocket 消息。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsInbound {

    /** JOIN / SIT_DOWN / STAND_UP / START_GAME / ACTION / LEAVE / HEARTBEAT */
    private String type;

    private Long roomId;
    private Integer seat;
    private String action;
    private BigDecimal amount;
    private BigDecimal buyIn;
    private String roomNo;
}
