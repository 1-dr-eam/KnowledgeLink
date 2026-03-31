package com.github.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.common.dto.Result;
import com.github.user.dto.LoginDTO;
import com.github.user.dto.UserInfoDTO;
import com.github.user.entity.User;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author ning
 * @date 2026/03/25
 */
public interface IUserService extends IService<User> {
    Result registerUser(User user);
    Result loginUser(LoginDTO loginDTO);
    Result logoutUser();
    Result updateUser(UserInfoDTO userInfoDTO);
    Result deleteUser();
    Result getCurrentUser();
    Result getUserInfoById(Long id);
    Result getUserFollowStatById(Long id);
    Result getUsersByIds(List<Long> ids);
}
