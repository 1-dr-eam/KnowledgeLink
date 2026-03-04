package com.liuyi.fateqq.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
public class CustomHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String userIdParam = httpRequest.getParameter("user_id");
            if (userIdParam != null) {
                try {
                    Integer userId = Integer.parseInt(userIdParam);
                    log.info("从查询参数获取用户: {}", userId);

                    // 创建Session并设置用户
                    HttpSession session = httpRequest.getSession(true);
                    session.setAttribute("user_id", userId);

                    attributes.put("HTTP_SESSION", session);
                    attributes.put("user_id", userId);

                    return true;
                } catch (NumberFormatException e) {
                    log.error("用户ID格式错误: {}", userIdParam);
                }
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        if (exception != null) {
            log.error("WebSocket握手失败", exception);
        } else {
            log.info("WebSocket握手成功");
        }
    }
}