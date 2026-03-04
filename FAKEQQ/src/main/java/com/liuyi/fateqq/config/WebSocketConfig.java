package com.liuyi.fateqq.config;

import com.liuyi.fateqq.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 使用不同的路径，避免与静态资源冲突
        registry.addHandler(chatWebSocketHandler, "/websocket/chat")
                .addInterceptors(new CustomHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }
}