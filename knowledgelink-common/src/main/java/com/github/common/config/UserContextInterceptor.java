package com.github.common.config;

import com.github.common.dto.UserDTO;
import com.github.common.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.github.common.constant.SecurityConstant.USER_HEADER_GRADE;
import static com.github.common.constant.SecurityConstant.USER_HEADER_ID;
import static com.github.common.constant.SecurityConstant.USER_HEADER_MAJOR;
import static com.github.common.constant.SecurityConstant.USER_HEADER_NAME;
import static com.github.common.constant.SecurityConstant.USER_HEADER_PHONE;

/**
 * 用户上下文拦截器
 *
 * @author ning
 * @date 2026/03/23
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String userIdHeader = request.getHeader(USER_HEADER_ID);
        if (!StringUtils.hasText(userIdHeader)) {
            return true;
        }
        UserDTO userDTO = new UserDTO();
        try {
            userDTO.setId(Long.valueOf(userIdHeader));
        } catch (Exception e) {
            UserHolder.removeUser();
            return true;
        }
        userDTO.setPhone(request.getHeader(USER_HEADER_PHONE));
        userDTO.setUsername(request.getHeader(USER_HEADER_NAME));
        userDTO.setMajor(request.getHeader(USER_HEADER_MAJOR));
        userDTO.setGrade(request.getHeader(USER_HEADER_GRADE));
        UserHolder.saveUser(userDTO);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
