package com.gamegroup.poker.ws;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 服务端 → 客户端的 WebSocket 消息（统一信封：{type, data}）。
 */
@Data
@Builder
public class WsOutbound {

    private String type;

    private Object data;

    public static WsOutbound of(String type, Object data) {
        return WsOutbound.builder().type(type).data(data).build();
    }
}
