package org.example.book.controller;

import com.alipay.easysdk.factory.Factory;
import io.swagger.v3.oas.annotations.Operation;
import learning_exchange_platform.model.User;
import org.example.book.entity.Result;
import org.example.book.service.PaymentService;
import org.example.book.util.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SessionManager sessionManager;

    @Operation(summary = "支付宝充值")
    @PostMapping("/alipay")
    public Result alipayRecharge(@RequestParam Double amount, HttpServletRequest request) {
        try {
            String notifyUrl = "http://z8669b6a.natappfree.cc/payment/alipay/notify";
            System.out.println("当前回调地址: " + notifyUrl);
            System.out.println("当前公网IP: " + request.getRemoteAddr());
            // 从session中获取当前用户ID，而不是从参数获取
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                return Result.error("用户未登录");
            }

            Integer userId = currentUser.getId();

            // 简单验证金额
            if (amount <= 0) {
                return Result.error("充值金额必须大于0");
            }
            if (amount > 100000) {
                return Result.error("单次充值金额不能超过100000元");
            }

            String payForm = paymentService.createAlipayOrder(userId, amount);
            // 返回支付宝支付页面HTML
            return Result.success(payForm);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建充值订单失败: " + e.getMessage());
        }
    }

    @Operation(summary = "支付宝支付通知回调")
    @RequestMapping(value = "/alipay/notify", method = {RequestMethod.POST, RequestMethod.GET, RequestMethod.HEAD})
    public String alipayNotify(HttpServletRequest request) {
        System.out.println("=== 收到支付宝回调请求 ===");
        System.out.println("时间: " + new java.util.Date());
        System.out.println("请求方法: " + request.getMethod());
        System.out.println("请求URL: " + request.getRequestURL());

        try {
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();

            System.out.println("回调参数:");
            for (String name : requestParams.keySet()) {
                String[] values = requestParams.get(name);
                String valueStr = String.join(",", values);
                params.put(name, valueStr);
                System.out.println("   " + name + ": " + valueStr);
            }

            if (params.isEmpty()) {
                System.err.println("没有收到任何参数");
                return "failure";
            }

            // 验证签名
            System.out.println("开始验证支付宝签名...");
            boolean signVerified = Factory.Payment.Common().verifyNotify(params);

            if (signVerified) {
                System.out.println("支付宝签名验证成功");

                String outTradeNo = params.get("out_trade_no");
                String tradeNo = params.get("trade_no");
                String tradeStatus = params.get("trade_status");
                String totalAmount = params.get("total_amount");

                System.out.println("交易信息:");
                System.out.println("   订单号: " + outTradeNo);
                System.out.println("   支付宝交易号: " + tradeNo);
                System.out.println("   交易状态: " + tradeStatus);
                System.out.println("   金额: " + totalAmount);

                // 只处理交易成功的通知
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    System.out.println("开始处理支付成功逻辑...");
                    boolean result = paymentService.handleAlipayNotify(outTradeNo, tradeNo);
                    if (result) {
                        System.out.println("支付处理成功完成");
                        return "success";
                    } else {
                        System.err.println("支付处理失败");
                        return "failure";
                    }
                } else {
                    System.out.println("交易未成功，状态: " + tradeStatus);
                    return "success"; // 支付宝要求对非成功状态也返回success
                }
            } else {
                System.err.println("支付宝签名验证失败");
                return "failure";
            }
        } catch (Exception e) {
            System.err.println("处理支付宝回调异常: " + e.getMessage());
            e.printStackTrace();
            return "failure";
        }
    }
}