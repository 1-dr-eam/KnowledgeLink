package com.github.trade.dto;

import lombok.Data;

@Data
public class UserAddressAddDTO {
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;
}
