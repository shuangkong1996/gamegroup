package com.gamegroup.poker.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话注册表：管理 session 与 userId / roomId 的绑定关系。
 */
@Slf4j
@Component
public class SessionRegistry {

    /** roomId -> (sessionId -> session) */
    private final Map<Long, Map<String, WebSocketSession>> byRoom = new ConcurrentHashMap<>();

    /** sessionId -> roomId（JOIN 后绑定） */
    private final Map<String, Long> roomOfSession = new ConcurrentHashMap<>();

    /** sessionId -> userId（握手时绑定） */
    private final Map<String, Long> userOfSession = new ConcurrentHashMap<>();

    public void register(WebSocketSession session, Long userId) {
        userOfSession.put(session.getId(), userId);
    }

    public void bindRoom(WebSocketSession session, Long roomId) {
        String sid = session.getId();
        roomOfSession.put(sid, roomId);
        byRoom.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(sid, session);
    }

    public void unbindRoom(WebSocketSession session) {
        String sid = session.getId();
        Long roomId = roomOfSession.remove(sid);
        if (roomId != null) {
            Map<String, WebSocketSession> sessions = byRoom.get(roomId);
            if (sessions != null) {
                sessions.remove(sid);
                if (sessions.isEmpty()) {
                    byRoom.remove(roomId);
                }
            }
        }
    }

    public void unregister(WebSocketSession session) {
        unbindRoom(session);
        userOfSession.remove(session.getId());
    }

    public Long userIdOf(WebSocketSession session) {
        return userOfSession.get(session.getId());
    }

    public boolean isInRoom(WebSocketSession session, Long roomId) {
        return roomId.equals(roomOfSession.get(session.getId()));
    }

    /**
     * 向房间内所有会话广播消息。
     */
    public void broadcast(Long roomId, WsOutbound message) {
        Map<String, WebSocketSession> sessions = byRoom.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String payload;
        try {
            payload = WsJson.toJson(message);
        } catch (Exception e) {
            log.error("序列化消息失败: type={}", message.getType(), e);
            return;
        }
        Set<String> dead = ConcurrentHashMap.newKeySet();
        for (WebSocketSession session : sessions.values()) {
            if (!session.isOpen()) {
                dead.add(session.getId());
                continue;
            }
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.warn("发送消息失败 session={}", session.getId());
                dead.add(session.getId());
            }
        }
        for (String sid : dead) {
            sessions.remove(sid);
        }
    }

    /**
     * 按用户视角向房间广播快照：每个会话用其 userId 生成个性化 data（如底牌隐私）。
     */
    public void broadcastSnapshot(Long roomId, java.util.function.Function<Long, Object> payloadFactory) {
        Map<String, WebSocketSession> sessions = byRoom.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        java.util.Set<String> dead = ConcurrentHashMap.newKeySet();
        for (WebSocketSession session : sessions.values()) {
            if (!session.isOpen()) {
                dead.add(session.getId());
                continue;
            }
            Long userId = userOfSession.get(session.getId());
            if (userId == null) {
                continue;
            }
            try {
                Object data = payloadFactory.apply(userId);
                session.sendMessage(new TextMessage(WsJson.toJson(WsOutbound.of("TABLE_SNAPSHOT", data))));
            } catch (Exception e) {
                log.warn("广播快照失败 session={}", session.getId());
                dead.add(session.getId());
            }
        }
        for (String sid : dead) {
            sessions.remove(sid);
        }
    }

    /**
     * 向指定用户发送消息。
     */
    public void sendToUser(Long roomId, Long userId, WsOutbound message) {
        Map<String, WebSocketSession> sessions = byRoom.get(roomId);
        if (sessions == null) {
            return;
        }
        for (WebSocketSession session : sessions.values()) {
            if (userId.equals(userOfSession.get(session.getId()))) {
                try {
                    session.sendMessage(new TextMessage(WsJson.toJson(message)));
                } catch (Exception e) {
                    log.warn("发送用户消息失败 userId={}", userId, e);
                }
                return;
            }
        }
    }
}
