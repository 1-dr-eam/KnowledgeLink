package com.liuyi.fateqq.service;

import com.liuyi.fateqq.mapper.ChatOrderMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatOrderService {

    @Autowired
    private ChatOrderMapper chatOrderMapper;

    //通过Id获取订单信息
    public Integer getOrderById(Integer orderId) {
        try {
            return chatOrderMapper.getOrderById(orderId);
        } catch (Exception e) {
            System.out.println("从数据库获取订单信息失败" + e.getMessage());
            return null;
        }
    }
}