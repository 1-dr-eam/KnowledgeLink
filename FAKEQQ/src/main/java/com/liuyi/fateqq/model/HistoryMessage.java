package com.liuyi.fateqq.model;

import lombok.Data;

import java.util.Date;

@Data
public class HistoryMessage {
    private Integer id;
    private String type;
    private String content;
    private String sender;
    private Date sendTime;
}
