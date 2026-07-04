package com.github.user.controller;

import com.github.common.dto.Result;
import com.github.user.dto.LoginDTO;
import com.github.user.dto.UserInfoDTO;
import com.github.user.entity.User;
import com.github.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户Controller
 *
 * @author ning
 * @date 2026/03/25
 */
@RestController
public class UserController {

    @Autowired
    private IUserService userService;

    @PostMapping("/register")
    public Result registerUser(@RequestBody User user) {
        return userService.registerUser(user);
    }

    @PostMapping("/login")
    public Result loginUser(@RequestBody LoginDTO loginDTO) {
        return userService.loginUser(loginDTO);
    }

    @GetMapping("/logout")
    public Result logoutUser() {
        return userService.logoutUser();
    }

    @PutMapping("/update")
    public Result updateUser(@RequestBody UserInfoDTO userInfoDTO) {
        return userService.updateUser(userInfoDTO);
    }

    @DeleteMapping("/delete")
    public Result deleteUser() {
        return userService.deleteUser();
    }

    @GetMapping("/current")
    public Result getCurrentUser() {
        return userService.getCurrentUser();
    }

    @GetMapping("/info")
    public Result getUserInfoById(@RequestParam("id") Long id) {
        return userService.getUserInfoById(id);
    }

    @GetMapping("/follow/stat")
    public Result getUserFollowStatById(@RequestParam("id") Long id) {
        return userService.getUserFollowStatById(id);
    }

    @GetMapping("/batch")
    public Result getUsersByIds(@RequestParam("ids") List<Long> ids) {
        return userService.getUsersByIds(ids);
    }

    @GetMapping("/profile/behavior")
    public Result getUserPreferenceBehavior(@RequestParam(value = "userId", required = false) Long userId) {
        return userService.getUserPreferenceBehavior(userId);
    }

    @GetMapping("/dashboard")
    public Result getUserDashboard() {
        return userService.getUserDashboard();
    }
}
