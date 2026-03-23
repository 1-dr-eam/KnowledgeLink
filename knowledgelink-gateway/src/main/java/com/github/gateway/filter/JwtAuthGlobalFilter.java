package com.github.gateway.filter;

import cn.hutool.json.JSONUtil;
import com.github.common.dto.Result;
import com.github.common.utils.JwtTokenUtil;
import com.github.gateway.config.GatewayAuthProperties;
import io.jsonwebtoken.Claims;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static com.github.common.constant.SecurityConstant.USER_HEADER_GRADE;
import static com.github.common.constant.SecurityConstant.USER_HEADER_ID;
import static com.github.common.constant.SecurityConstant.USER_HEADER_MAJOR;
import static com.github.common.constant.SecurityConstant.USER_HEADER_NAME;
import static com.github.common.constant.SecurityConstant.USER_HEADER_PHONE;
import static com.github.common.constant.SecurityConstant.JWT_CLAIM_GRADE;
import static com.github.common.constant.SecurityConstant.JWT_CLAIM_MAJOR;
import static com.github.common.constant.SecurityConstant.JWT_CLAIM_PHONE;
import static com.github.common.constant.SecurityConstant.JWT_CLAIM_USER_ID;
import static com.github.common.constant.SecurityConstant.JWT_CLAIM_USERNAME;

/**
 * jWT认证过滤器
 *
 * @author ning
 * @date 2026/03/23
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {
    private final GatewayAuthProperties gatewayAuthProperties;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * jWT认证过滤器构造函数
     *
     * @param gatewayAuthProperties 网关鉴权配置
     * @param jwtTokenUtil jWT工具类
     */
    public JwtAuthGlobalFilter(GatewayAuthProperties gatewayAuthProperties, JwtTokenUtil jwtTokenUtil) {
        this.gatewayAuthProperties = gatewayAuthProperties;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 全局过滤器处理入口
     *
     * @param exchange 请求上下文
     * @param chain 过滤器链
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!gatewayAuthProperties.isEnabled()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }
        String headerValue = exchange.getRequest().getHeaders().getFirst(gatewayAuthProperties.getHeaderName());
        if (headerValue == null || headerValue.isBlank()) {
            return writeError(exchange.getResponse(), "令牌缺失");
        }
        String tokenPrefix = gatewayAuthProperties.getTokenPrefix();
        String token;
        if (headerValue.startsWith(tokenPrefix + " ")) {
            token = headerValue.substring(tokenPrefix.length() + 1);
        } else {
            token = headerValue;
        }
        JwtCheckResult jwtCheckResult = validateAndParseJwt(token);
        if (!jwtCheckResult.success) {
            return writeError(exchange.getResponse(), jwtCheckResult.errorMsg);
        }
        Claims claims = jwtCheckResult.claims;
        String tokenId = claims.getId();
        Object userIdObj = claims.get(JWT_CLAIM_USER_ID);
        String userId = userIdObj == null ? "" : String.valueOf(userIdObj);
        String phone = (String) claims.get(JWT_CLAIM_PHONE);
        String username = (String) claims.get(JWT_CLAIM_USERNAME);
        String major = (String) claims.get(JWT_CLAIM_MAJOR);
        String grade = (String) claims.get(JWT_CLAIM_GRADE);
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(USER_HEADER_ID, userId)
                .header(USER_HEADER_PHONE, phone == null ? "" : phone)
                .header(USER_HEADER_NAME, username == null ? "" : username)
                .header(USER_HEADER_MAJOR, major == null ? "" : major)
                .header(USER_HEADER_GRADE, grade == null ? "" : grade)
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();
        mutatedExchange.getAttributes().put("gateway.tokenId", tokenId);
        return chain.filter(mutatedExchange)
                .doOnSuccess(unused -> refreshTokenSession(mutatedExchange, tokenId));
    }

    /**
     * 获取过滤器顺序
     *
     * @return 顺序值
     */
    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 判断当前路径是否在白名单中
     *
     * @param path 请求路径
     * @return 是否白名单路径
     */
    private boolean isWhitePath(String path) {
        for (String whitePath : gatewayAuthProperties.getWhitelist()) {
            if (whitePath.endsWith("/**")) {
                String prefix = whitePath.substring(0, whitePath.length() - 3);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (path.equals(whitePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 转发成功后刷新令牌会话时间
     *
     * @param exchange 请求上下文
     * @param tokenId 令牌ID
     */
    private void refreshTokenSession(ServerWebExchange exchange, String tokenId) {
        HttpStatus status = (HttpStatus) exchange.getResponse().getStatusCode();
        if (status == null || status.is2xxSuccessful() || status.is3xxRedirection()) {
            jwtTokenUtil.refreshTokenSession(tokenId);
        }
    }

    /**
     * 验证并解析令牌
     *
     * @param token 令牌字符串
     * @return 令牌校验结果
     */
    private JwtCheckResult validateAndParseJwt(String token) {
        if (!jwtTokenUtil.isTokenValid(token)) {
            return JwtCheckResult.error("令牌非法或签名错误");
        }
        Claims claims;
        try {
            claims = jwtTokenUtil.parseToken(token);
        } catch (Exception e) {
            return JwtCheckResult.error("令牌解析失败");
        }
        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank() || !jwtTokenUtil.isTokenSessionValid(tokenId)) {
            return JwtCheckResult.error("令牌已过期");
        }
        return JwtCheckResult.success(claims);
    }

    /**
     * 统一输出鉴权失败响应
     *
     * @param response 响应对象
     * @param msg 错误信息
     * @return Mono<Void>
     */
    private Mono<Void> writeError(ServerHttpResponse response, String msg) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result result = Result.error(msg);
        byte[] data = JSONUtil.toJsonStr(result).getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(data)));
    }

    /**
     * 令牌校验结果对象
     */
    private static class JwtCheckResult {
        private final boolean success;
        private final Claims claims;
        private final String errorMsg;

        /**
         * 令牌校验结果构造函数
         *
         * @param success 是否校验成功
         * @param claims 解析后的负载
         * @param errorMsg 失败信息
         */
        private JwtCheckResult(boolean success, Claims claims, String errorMsg) {
            this.success = success;
            this.claims = claims;
            this.errorMsg = errorMsg;
        }

        /**
         * 构建成功结果
         *
         * @param claims 解析后的负载
         * @return 令牌校验结果
         */
        private static JwtCheckResult success(Claims claims) {
            return new JwtCheckResult(true, claims, null);
        }

        /**
         * 构建失败结果
         *
         * @param errorMsg 失败信息
         * @return 令牌校验结果
         */
        private static JwtCheckResult error(String errorMsg) {
            return new JwtCheckResult(false, null, errorMsg);
        }
    }
}
