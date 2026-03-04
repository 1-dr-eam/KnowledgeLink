package org.example.book.util;

import learning_exchange_platform.model.User;
import learning_exchange_platform.service.UserService;
import learning_exchange_platform.utils.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Component
public class SessionManager {

    private static final String USER_SESSION_KEY = "currentUser";
    private static final String ADMIN_SESSION_KEY = "currentAdmin";
    private static final String SUPER_ADMIN_SESSION_KEY = "currentSuperAdmin";
    @Autowired
    private UserService userService;

    /**
     * 获取当前请求的HttpSession
     */
    private HttpSession getSession() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        return request.getSession();
    }

    /**
     * 设置用户Session
     */
    public void setUserSession(User user) {
        HttpSession session = getSession();
        session.setAttribute(USER_SESSION_KEY, user);
    }
    /**
     * 获取当前登录用户
     */
    public User getCurrentUser() {
        int user_id= (int) SessionUtil.getSession().getAttribute("user_id");
        return userService.getUserInfo(user_id);
    }


    /**
     * 检查用户是否登录
     */
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    /**
     * 检查当前用户是否为指定用户
     */
    public boolean isCurrentUser(Integer userId) {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getId().equals(userId);
    }

    /**
     * 获取当前用户ID
     */
    public Integer getCurrentUserId() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    /**
     * 用户登出
     */
    public void userLogout() {
        HttpSession session = getSession();
        session.removeAttribute(USER_SESSION_KEY);
    }

    /**
     * 管理员登出
     */
    public void adminLogout() {
        HttpSession session = getSession();
        session.removeAttribute(ADMIN_SESSION_KEY);
    }

    /**
     * 超级管理员登出
     */
    public void superAdminLogout() {
        HttpSession session = getSession();
        session.removeAttribute(SUPER_ADMIN_SESSION_KEY);
    }

    /**
     * 完全登出（清除所有Session）
     */
    public void completeLogout() {
        HttpSession session = getSession();
        session.removeAttribute(USER_SESSION_KEY);
        session.removeAttribute(ADMIN_SESSION_KEY);
        session.removeAttribute(SUPER_ADMIN_SESSION_KEY);
        session.invalidate();
    }

    /**
     * 设置Session属性
     */
    public void setAttribute(String key, Object value) {
        HttpSession session = getSession();
        session.setAttribute(key, value);
    }

    /**
     * 获取Session属性
     */
    public Object getAttribute(String key) {
        HttpSession session = getSession();
        return session.getAttribute(key);
    }

    /**
     * 移除Session属性
     */
    public void removeAttribute(String key) {
        HttpSession session = getSession();
        session.removeAttribute(key);
    }

    /**
     * 使Session失效
     */
    public void invalidateSession() {
        HttpSession session = getSession();
        session.invalidate();
    }

    /**
     * 获取Session创建时间
     */
    public long getCreationTime() {
        HttpSession session = getSession();
        return session.getCreationTime();
    }

    /**
     * 获取Session最后访问时间
     */
    public long getLastAccessedTime() {
        HttpSession session = getSession();
        return session.getLastAccessedTime();
    }

    /**
     * 获取Session最大不活动间隔（秒）
     */
    public int getMaxInactiveInterval() {
        HttpSession session = getSession();
        return session.getMaxInactiveInterval();
    }

    /**
     * 设置Session最大不活动间隔（秒）
     */
    public void setMaxInactiveInterval(int interval) {
        HttpSession session = getSession();
        session.setMaxInactiveInterval(interval);
    }
}