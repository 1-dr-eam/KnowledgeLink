package com.github.common.utils;

import cn.hutool.json.JSONUtil;
import com.github.common.config.JwtProperties;
import com.github.common.constant.SecurityConstant;
import com.github.common.dto.JwtUserClaimsDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JWT令牌工具类
 *
 * @author ning
 * @date 2026/03/23
 */
@Component
public class JwtTokenUtil {
    public static final long TOKEN_EXPIRE_MINUTES = 180L;

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;

    public JwtTokenUtil(JwtProperties jwtProperties, StringRedisTemplate stringRedisTemplate) {
        this.jwtProperties = jwtProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 令牌会话对象
     */
    @Setter
    @Getter
    static class TokenSession {
        private LocalDateTime createTime;
        private LocalDateTime expireTime;

    }

    /**
     * 生成令牌
     *
     * @param userClaims 用户Claims实体类
     * @return jWT令牌
     */
    public String generateToken(JwtUserClaimsDTO userClaims) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusMinutes(TOKEN_EXPIRE_MINUTES);
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstant.JWT_CLAIM_USER_ID, userClaims.getUserId());
        claims.put(SecurityConstant.JWT_CLAIM_PHONE, userClaims.getPhone());
        claims.put(SecurityConstant.JWT_CLAIM_USERNAME, userClaims.getUsername());
        claims.put(SecurityConstant.JWT_CLAIM_MAJOR, userClaims.getMajor());
        claims.put(SecurityConstant.JWT_CLAIM_GRADE, userClaims.getGrade());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claims(claims)
                .issuedAt(toDate(now))
                .expiration(toDate(expireTime))
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析令牌
     *
     * @param token 令牌
     * @return 载荷内容claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    /**
     * 验证jWT令牌格式与签名有效性
     *
     * @param token 令牌
     * @return boolean
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取令牌ID
     *
     * @param token 令牌
     * @return 令牌ID
     */
    public String getTokenId(String token) {
        return parseToken(token).getId();
    }

    /**
     * 保存令牌会话信息
     *
     * @param tokenId 令牌ID
     * @param createTime 创建时间
     */
    public void saveTokenSession(String tokenId, LocalDateTime createTime) {
        LocalDateTime expireTime = createTime.plusMinutes(TOKEN_EXPIRE_MINUTES);
        TokenSession tokenSession = new TokenSession();
        tokenSession.setCreateTime(createTime);
        tokenSession.setExpireTime(expireTime);
        String sessionJson = JSONUtil.toJsonStr(tokenSession);
        stringRedisTemplate.opsForValue().set(buildSessionKey(tokenId), sessionJson, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 验证令牌会话时间有效性
     *
     * @param tokenId 令牌ID
     * @return boolean
     */
    public boolean isTokenSessionValid(String tokenId) {
        String sessionJson = stringRedisTemplate.opsForValue().get(buildSessionKey(tokenId));
        if (sessionJson == null || sessionJson.isBlank()) {
            return false;
        }
        TokenSession tokenSession = JSONUtil.toBean(sessionJson, TokenSession.class);
        if (tokenSession.getExpireTime() == null) {
            return false;
        }
        return tokenSession.getExpireTime().atZone(ZoneId.systemDefault()).toInstant().isAfter(Instant.now());
    }

    /**
     * 刷新令牌会话过期时间
     *
     * @param tokenId 令牌ID
     */
    public void refreshTokenSession(String tokenId) {
        String sessionJson = stringRedisTemplate.opsForValue().get(buildSessionKey(tokenId));
        if (sessionJson == null || sessionJson.isBlank()) {
            return;
        }
        TokenSession tokenSession = JSONUtil.toBean(sessionJson, TokenSession.class);
        tokenSession.setExpireTime(LocalDateTime.now().plusMinutes(TOKEN_EXPIRE_MINUTES));
        stringRedisTemplate.opsForValue().set(buildSessionKey(tokenId), JSONUtil.toJsonStr(tokenSession), TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 删除令牌会话
     *
     * @param tokenId 令牌ID
     */
    public void removeTokenSession(String tokenId) {
        stringRedisTemplate.delete(buildSessionKey(tokenId));
    }

    /**
     * 构建令牌会话缓存键
     *
     * @param tokenId 令牌ID
     * @return 缓存键
     */
    private String buildSessionKey(String tokenId) {
        return SecurityConstant.JWT_TOKEN_SESSION_KEY + tokenId;
    }

    /**
     * 获取签名密钥
     *
     * @return HMAC签名密钥
     */
    private SecretKey getSecretKey() {
        if (jwtProperties.getSecret() == null || jwtProperties.getSecret().isBlank()) {
            throw new IllegalArgumentException("jwt.secret未配置");
        }
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * LocalDateTime转换为Date
     *
     * @param localDateTime 本地时间
     * @return 日期对象
     */
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
