package com.github.trade.controller;

import com.github.common.dto.Result;
import com.github.trade.dto.IdRequest;
import com.github.trade.util.weChatPaymentUtil;
import org.springframework.web.bind.annotation.*;

/**
 * 微信支付控制器
 *
 * @author ning
 * @date 2026/03/23
 */
@RestController
public class WeChatPaymentController {
    private final weChatPaymentUtil weChatPaymentUtil;

    public WeChatPaymentController(weChatPaymentUtil weChatPaymentUtil) {
        this.weChatPaymentUtil = weChatPaymentUtil;
    }

    /**
     * 创建微信支付单
     *
     * @param idRequest 订单id请求参数
     * @return 支付单信息
     */
    @PostMapping("/createWechatPay")
    public Result createWechatPay(@RequestBody IdRequest idRequest) {
        return weChatPaymentUtil.createPayOrder(idRequest);
    }

    /**
     * 生成微信支付二维码
     *
     * @param idRequest 订单id请求参数
     * @return 二维码信息
     */
    @PostMapping("/generateWechatPayQrCode")
    public Result generateWechatPayQrCode(@RequestBody IdRequest idRequest) {
        return weChatPaymentUtil.generateQrCode(idRequest);
    }

    /**
     * 触发模拟微信支付
     *
     * @param idRequest 订单id请求参数
     * @return 处理结果
     */
    @PostMapping("/mockWechatPay")
    public Result mockWechatPay(@RequestBody IdRequest idRequest) {
        return weChatPaymentUtil.mockPay(idRequest);
    }
}
