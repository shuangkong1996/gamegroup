package com.gamegroup.poker.ws;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * WebSocket 消息的 JSON 序列化工具。
 */
public final class WsJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private WsJson() {
    }

    public static String toJson(Object obj) throws Exception {
        return MAPPER.writeValueAsString(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return MAPPER.readValue(json, clazz);
    }
}
