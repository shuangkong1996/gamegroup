package com.gamegroup.poker.ws;

import com.gamegroup.poker.service.PokerGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Poker WebSocket 消息处理器：握手后注册会话，文本消息路由到游戏服务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PokerWebSocketHandler extends TextWebSocketHandler {

    private final SessionRegistry sessionRegistry;
    private final PokerGameService pokerGameService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (!(userId instanceof Long uid)) {
            log.warn("WebSocket 连接缺少 userId，关闭 session={}", session.getId());
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception e) {
                log.warn("关闭连接失败", e);
            }
            return;
        }
        sessionRegistry.register(session, uid);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = sessionRegistry.userIdOf(session);
        if (userId == null) {
            return;
        }
        try {
            WsInbound inbound = WsJson.fromJson(message.getPayload(), WsInbound.class);
            pokerGameService.handleMessage(session, userId, inbound);
        } catch (Exception e) {
            log.error("处理 WS 消息失败 userId={}", userId, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = sessionRegistry.userIdOf(session);
        sessionRegistry.unregister(session);
        if (userId != null) {
            pokerGameService.onDisconnect(userId);
        }
    }
}
