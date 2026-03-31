package com.github.chat.config;

import com.github.chat.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 网络聊天配置
 *
 * @author ning
 * @date 2026/03/24
 */
@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;
    @Autowired
    private JwtHandshakeInterceptor jwtHandshakeInterceptor;

    /**
     * 注册 WebSocket 处理器与握手拦截器
     *
     * @param registry WebSocket 处理器注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/websocket/chat")
                .setAllowedOrigins("*")
                .addInterceptors(jwtHandshakeInterceptor);
    }
}
