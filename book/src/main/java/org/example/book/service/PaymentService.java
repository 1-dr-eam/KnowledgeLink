package org.example.book.service;

import com.alipay.easysdk.factory.Factory;
import org.example.book.entity.Recharge;
import org.example.book.mapper.RechargeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class PaymentService{

    @Autowired
    private RechargeMapper rechargeMapper;

    
    public String createAlipayOrder(Integer userId, Double amount) {
        try {
            String outTradeNo = "ALIPAY_" + System.currentTimeMillis() + "_" + userId;

            Recharge recharge = new Recharge(userId, amount, outTradeNo, "待支付");
            rechargeMapper.insert(recharge);

            // 显式指定异步通知地址
            com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse response =
                    Factory.Payment.Page()
                            .asyncNotify("http://z8669b6a.natappfree.cc/payment/alipay/notify")  // 添加这行
                            .pay("用户充值", outTradeNo, amount.toString(), "");

            System.out.println("创建支付宝订单，订单号: " + outTradeNo);
            System.out.println("异步通知地址: http://z8669b6a.natappfree.cc/payment/alipay/notify");

            return response.body;
        } catch (Exception e) {
            throw new RuntimeException("创建支付宝订单失败: " + e.getMessage());
        }
    }

    
    @Transactional
    public boolean handleAlipayNotify(String outTradeNo, String tradeNo) {
        try {
            // 根据outTradeNo查询充值记录
            Recharge recharge = rechargeMapper.selectByOutTradeNo(outTradeNo);
            if (recharge == null) {
                System.err.println("未找到充值记录: " + outTradeNo);
                return false;
            }

            // 检查是否已经处理过
            if ("支付成功".equals(recharge.getStatus())) {
                System.out.println("充值记录已处理: " + outTradeNo);
                return true;
            }

            // 更新充值记录状态
            recharge.setStatus("支付成功");
            recharge.setTradeNo(tradeNo);
            recharge.setUpdateTime(LocalDateTime.now());
            rechargeMapper.updateById(recharge);

            // 使用 rechargeMapper 更新用户余额
            int result = rechargeMapper.updateUserBalance(recharge.getUserId(), recharge.getAmount());

            if (result > 0) {
                System.out.println("支付宝回调处理成功: " + outTradeNo + ", 用户: " + recharge.getUserId() + ", 金额: " + recharge.getAmount());
                return true;
            } else {
                System.err.println("更新用户余额失败: " + recharge.getUserId());
                return false;
            }
        } catch (Exception e) {
            System.err.println("处理支付宝通知失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}