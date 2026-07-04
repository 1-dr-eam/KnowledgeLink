package com.github.trade.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * @author ning
 * @date 2026-03-19
 */
@Data
public class OrderSynoVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String image;
    private String bookName;
    private String bookAuthor;
    private String bookPublisher;
    private String bookVersion;
    private Integer count;
    private Double price;
    private Double totalPrice;
    private String status;
}
