package com.gamegroup.poker.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class PokerWebSocketConfig implements WebSocketConfigurer {

    private final PokerWebSocketHandler pokerWebSocketHandler;
    private final PokerHandshakeInterceptor pokerHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pokerWebSocketHandler, "/ws/poker")
                .addInterceptors(pokerHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
