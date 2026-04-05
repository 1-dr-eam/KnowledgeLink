package com.github.trade.util;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import cn.hutool.json.JSONUtil;
import com.github.common.dto.Result;
import com.github.common.mq.MqMessageDTO;
import com.github.common.mq.MqTopologyConstant;
import com.github.common.utils.UserHolder;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.WechatPayCallbackDTO;
import com.github.trade.entity.Order;
import com.github.trade.mapper.ConsumerOrderMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 微信模拟支付工具类
 *
 * @author ning
 * @date 2026-03-23
 */
@Component
public class weChatPaymentUtil {
    private static final String ORDER_STATUS_WAIT_PAY = "待支付";
    private static final String ORDER_STATUS_WAIT_SHIP = "待发货";
    private static final String WECHAT_MOCK_PAY_PREFIX = "weixin://wxpay/bizpayurl?pr=KL";
    private final ConsumerOrderMapper consumerOrderMapper;
    private final RabbitTemplate rabbitTemplate;

    public weChatPaymentUtil(ConsumerOrderMapper consumerOrderMapper, RabbitTemplate rabbitTemplate) {
        this.consumerOrderMapper = consumerOrderMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 创建支付订单信息
     *
     * @param idRequest 订单id请求参数
     * @return 包含订单金额、支付链接与交易号
     */
    public Result createPayOrder(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("订单参数错误");
        }
        Order order = consumerOrderMapper.selectById(idRequest.getId());
        if (order == null || !UserHolder.getUser().getId().equals(order.getUserId())) {
            return Result.error("订单不存在");
        }
        if (!ORDER_STATUS_WAIT_PAY.equals(order.getStatus())) {
            return Result.error("当前状态不可支付");
        }
        String codeUrl = buildCodeUrl(order.getId());
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("orderId", order.getId());
        resultData.put("totalPrice", order.getTotalPrice());
        resultData.put("codeUrl", codeUrl);
        resultData.put("tradeNo", "WX" + order.getId());
        return Result.success(resultData);
    }

    /**
     * 生成支付二维码信息
     *
     * @param idRequest 订单id请求参数
     * @return 包含二维码Base64与支付链接
     */
    public Result generateQrCode(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("订单参数错误");
        }
        Order order = consumerOrderMapper.selectById(idRequest.getId());
        if (order == null || !UserHolder.getUser().getId().equals(order.getUserId())) {
            return Result.error("订单不存在");
        }
        if (!ORDER_STATUS_WAIT_PAY.equals(order.getStatus())) {
            return Result.error("当前状态不可生成支付二维码");
        }
        String codeUrl = buildCodeUrl(order.getId());
        QrConfig qrConfig = new QrConfig(260, 260);
        String qrCodeBase64 = QrCodeUtil.generateAsBase64(codeUrl, qrConfig, "png");
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("orderId", order.getId());
        resultData.put("codeUrl", codeUrl);
        resultData.put("qrCode", qrCodeBase64);
        return Result.success(resultData);
    }

    /**
     * 模拟支付成功并发送支付成功消息
     *
     * @param idRequest 订单id请求参数
     * @return 处理结果
     */
    public Result mockPay(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("订单参数错误");
        }
        Order order = consumerOrderMapper.selectById(idRequest.getId());
        if (order == null || !UserHolder.getUser().getId().equals(order.getUserId())) {
            return Result.error("订单不存在");
        }
        if (!ORDER_STATUS_WAIT_PAY.equals(order.getStatus())) {
            return Result.error("当前状态不可模拟支付");
        }
        Map<String, Object> payMessage = new HashMap<>();
        payMessage.put("orderId", order.getId());
        payMessage.put("targetStatus", ORDER_STATUS_WAIT_SHIP);
        MqMessageDTO mqMessageDTO = new MqMessageDTO();
        mqMessageDTO.setMessageId(UUID.randomUUID().toString());
        mqMessageDTO.setCreateTime(LocalDateTime.now());
        mqMessageDTO.setMessageBody(JSONUtil.toJsonStr(payMessage));
        rabbitTemplate.convertAndSend(MqTopologyConstant.PAY_SUCCESS_EXCHANGE, MqTopologyConstant.PAY_SUCCESS_ROUTING_KEY, mqMessageDTO);
        return Result.success();
    }

    public Result handlePayCallback(WechatPayCallbackDTO callbackDTO) {
        if (callbackDTO == null || callbackDTO.getTradeNo() == null || callbackDTO.getTradeNo().isBlank()) {
            return Result.error("回调参数错误");
        }
        if (callbackDTO.getPayStatus() != null && !"SUCCESS".equalsIgnoreCase(callbackDTO.getPayStatus())) {
            return Result.error("支付状态非成功");
        }
        if (!callbackDTO.getTradeNo().startsWith("WX")) {
            return Result.error("交易号格式错误");
        }
        Long orderId;
        try {
            orderId = Long.parseLong(callbackDTO.getTradeNo().substring(2));
        } catch (Exception e) {
            return Result.error("交易号解析失败");
        }
        Order order = consumerOrderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!ORDER_STATUS_WAIT_PAY.equals(order.getStatus())) {
            return Result.success();
        }
        Map<String, Object> payMessage = new HashMap<>();
        payMessage.put("orderId", order.getId());
        payMessage.put("targetStatus", ORDER_STATUS_WAIT_SHIP);
        MqMessageDTO mqMessageDTO = new MqMessageDTO();
        mqMessageDTO.setMessageId(UUID.randomUUID().toString());
        mqMessageDTO.setCreateTime(LocalDateTime.now());
        mqMessageDTO.setMessageBody(JSONUtil.toJsonStr(payMessage));
        rabbitTemplate.convertAndSend(MqTopologyConstant.PAY_SUCCESS_EXCHANGE, MqTopologyConstant.PAY_SUCCESS_ROUTING_KEY, mqMessageDTO);
        return Result.success();
    }

    /**
     * 构建微信模拟支付链接
     *
     * @param orderId 订单id
     * @return 支付链接
     */
    private String buildCodeUrl(Long orderId) {
        return WECHAT_MOCK_PAY_PREFIX + orderId;
    }
}
