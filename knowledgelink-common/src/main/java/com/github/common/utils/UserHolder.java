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
        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        return userDTO;
//        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
