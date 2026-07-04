package com.github.forum.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class UserRemoteClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String userBaseUrl;

    public UserRemoteClient(RestTemplate restTemplate,
                            ObjectMapper objectMapper,
                            @Value("${user.base-url:http://127.0.0.1:8086}") String userBaseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userBaseUrl = userBaseUrl;
    }

    public UserSimpleInfo getUserSimpleInfo(Long userId) {
        UserSimpleInfo userSimpleInfo = new UserSimpleInfo();
        if (userId == null) {
            return userSimpleInfo;
        }
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    userBaseUrl + "/info?id=" + userId,
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class
            );
            if (response.getBody() == null) {
                return userSimpleInfo;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.path("code").asInt() != 1) {
                return userSimpleInfo;
            }
            JsonNode data = root.path("data");
            userSimpleInfo.setUsername(readText(data, "username"));
            userSimpleInfo.setAvatar(readText(data, "avatar"));
            return userSimpleInfo;
        } catch (Exception e) {
            return userSimpleInfo;
        }
    }

    public Boolean isFollow(Long targetUserId) {
        if (targetUserId == null) {
            return Boolean.FALSE;
        }
        try {
            HttpHeaders headers = buildHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    userBaseUrl + "/isFollow?id=" + targetUserId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (response.getBody() == null) {
                return Boolean.FALSE;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.path("code").asInt() != 1) {
                return Boolean.FALSE;
            }
            return root.path("data").asBoolean(false);
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return headers;
        }
        HttpServletRequest request = attributes.getRequest();
        String authorization = request.getHeader("Authorization");
        if (authorization != null && !authorization.isBlank()) {
            headers.set("Authorization", authorization);
        }
        return headers;
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    public static class UserSimpleInfo {
        private String username;
        private String avatar;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
    }
}
