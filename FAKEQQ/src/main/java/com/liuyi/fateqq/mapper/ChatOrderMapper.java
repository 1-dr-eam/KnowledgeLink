package com.liuyi.fateqq.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatOrderMapper {
    //根据订单ID获取对应的订单关键信息
    // 方法返回类型改为Integer
    Integer getOrderById(Integer orderId);
}