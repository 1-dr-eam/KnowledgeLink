package org.example.book.service;

import org.example.book.mapper.BookUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookUserService {
    @Autowired
    private BookUserMapper bookUserMapper;

    
    public int recharge(Integer userId, Double amount) {
        return bookUserMapper.recharge(userId, amount);
    }

    
    public int withdraw(Integer userId, Double amount) {
        return bookUserMapper.withdraw(userId, amount);
    }

    
    public int updateUserAvatar(Integer userId, String avatarUrl) {
        if (userId == null || avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return 0; // 参数无效，返回0表示失败
        }
        return bookUserMapper.setUserAvatar(userId, avatarUrl);
    }


}