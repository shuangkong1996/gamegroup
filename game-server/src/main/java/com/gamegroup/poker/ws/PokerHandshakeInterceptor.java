package com.gamegroup.poker.ws;

import com.gamegroup.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器：从 query 参数 token 校验 JWT，并写入 userId。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PokerHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        String token = null;
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && "token".equals(kv[0])) {
                    token = kv[1];
                }
            }
        }
        if (token == null || token.isEmpty()) {
            log.warn("WebSocket 握手缺少 token");
            return false;
        }
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            attributes.put("userId", userId);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 握手 token 校验失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
