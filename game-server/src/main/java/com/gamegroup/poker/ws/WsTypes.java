package com.gamegroup.poker.ws;

/**
 * WebSocket 消息类型常量。
 */
public final class WsTypes {

    private WsTypes() {
    }

    // 入站
    public static final String JOIN = "JOIN";
    public static final String SIT_DOWN = "SIT_DOWN";
    public static final String STAND_UP = "STAND_UP";
    public static final String START_GAME = "START_GAME";
    public static final String ACTION = "ACTION";
    public static final String LEAVE = "LEAVE";
    public static final String HEARTBEAT = "HEARTBEAT";

    // 出站
    public static final String TABLE_SNAPSHOT = "TABLE_SNAPSHOT";
    public static final String ROOM_CLOSED = "ROOM_CLOSED";
    public static final String ERROR = "ERROR";
    public static final String PONG = "PONG";
}
