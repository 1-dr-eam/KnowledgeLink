package org.example.book.mapper;

import org.apache.ibatis.annotations.*;
import org.example.book.entity.Recharge;
import java.time.LocalDateTime;

@Mapper
public interface RechargeMapper {

    @Insert("INSERT INTO recharge (userid, amount, outTradeNo, tradeNo, status, createTime, updateTime) " +
            "VALUES (#{userId}, #{amount}, #{outTradeNo}, #{tradeNo}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Recharge recharge);

    @Select("SELECT * FROM recharge WHERE outTradeNo = #{outTradeNo}")
    Recharge selectByOutTradeNo(String outTradeNo);

    @Update("UPDATE recharge SET status=#{status}, tradeNo=#{tradeNo}, updateTime=#{updateTime} WHERE id=#{id}")
    int updateById(Recharge recharge);

    @Update("UPDATE user SET balance = balance + #{amount} WHERE id = #{userId}")
    int updateUserBalance(@Param("userId") Integer userId, @Param("amount") Double amount);
}