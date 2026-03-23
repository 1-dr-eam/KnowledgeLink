package com.github.common.utils;

import com.github.common.dto.UserDTO;

/**
 * 用户持有器，用于保存和获取当前登录用户
 * @author ningning
 * @date 2026/02/05
 */
public class UserHolder {
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO userDTO){
        tl.set(userDTO);
    }

    public static UserDTO getUser(){
        UserDTO userDTO = tl.get();
        if (userDTO != null) {
            return userDTO;
        }
        UserDTO defaultUser = new UserDTO();
        defaultUser.setId(1L);
        return defaultUser;
    }

    public static void removeUser(){
        tl.remove();
    }
}
