package com.github.trade.dto;

import lombok.Data;

@Data
public class WechatPayCallbackDTO {
    private String tradeNo;
    private String payStatus;
}
