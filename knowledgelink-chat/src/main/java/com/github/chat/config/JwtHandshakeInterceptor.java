package com.github.chat.config;

import com.github.common.utils.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import static com.github.common.constant.SecurityConstant.JWT_CLAIM_USER_ID;

/**
 * JWT 握手拦截器
 *
 * @author ning
 * @date 2026/03/24
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * 在 WebSocket 握手前校验用户身份并写入用户标识
     *
     * @param request 握手请求
     * @param response 握手响应
     * @param wsHandler WebSocket 处理器
     * @param attributes 会话属性
     * @return 校验通过返回 true
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletServerHttpRequest)) {
            return false;
        }
        String auth = servletServerHttpRequest.getServletRequest().getHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            return false;
        }
        String token = auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        if (!jwtTokenUtil.isTokenValid(token)) {
            return false;
        }
        Claims claims = jwtTokenUtil.parseToken(token);
        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank() || !jwtTokenUtil.isTokenSessionValid(tokenId)) {
            return false;
        }
        Object userIdObj = claims.get(JWT_CLAIM_USER_ID);
        if (userIdObj == null) {
            return false;
        }
        attributes.put("userId", Long.valueOf(String.valueOf(userIdObj)));
        return true;
    }

    /**
     * WebSocket 握手完成后的扩展回调
     *
     * @param request 握手请求
     * @param response 握手响应
     * @param wsHandler WebSocket 处理器
     * @param exception 握手异常
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
