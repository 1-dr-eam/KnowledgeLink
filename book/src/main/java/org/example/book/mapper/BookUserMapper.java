package org.example.book.mapper;

import learning_exchange_platform.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BookUserMapper {
    // 获取所有用户
    @Select("SELECT * FROM user")
    List<User> getAllUsers();

    // 充值功能
    @Update("UPDATE user SET balance = balance + #{amount} WHERE id = #{userId}")
    int recharge(@Param("userId") Integer userId, @Param("amount") Double amount);

    // 提现功能
    @Update("UPDATE user SET balance = balance - #{amount} WHERE id = #{userId} AND balance >= #{amount}")
    int withdraw(@Param("userId") Integer userId, @Param("amount") Double amount);

    // 更新用户余额
    @Update("UPDATE user SET balance = #{balance} WHERE id = #{userId}")
    int updateUserBalance(@Param("userId") Integer userId, @Param("balance") Double balance);

    // 增加用户余额
    @Update("UPDATE user SET balance = balance + #{amount} WHERE id = #{userId}")
    int addUserBalance(@Param("userId") Integer userId, @Param("amount") Double amount);

    // 转账给卖家
    @Update("UPDATE user SET balance = balance + #{amount} WHERE id = #{sellerId}")
    int transferToSeller(@Param("sellerId") Integer sellerId, @Param("amount") Double amount);

    @Update("UPDATE user SET avatar = #{avatar} WHERE id = #{id}")
    int setUserAvatar(@Param("id") Integer id, @Param("avatar") String avatar);
}