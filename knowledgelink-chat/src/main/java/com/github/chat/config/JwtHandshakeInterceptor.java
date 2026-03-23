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

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

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

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }
}
