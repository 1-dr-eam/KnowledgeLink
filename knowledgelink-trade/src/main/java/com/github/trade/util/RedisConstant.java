package com.github.trade.util;

/**
 * @author ning
 * @date 2026-03-10
 */
public class RedisConstant {
    public static final String BOOK_INFO_KEY = "KnowledgeLink:trade:bookInfo:";
    public static final Long BOOK_INFO_TTL = 360L;
    public static final String CART_KEY = "KnowledgeLink:trade:cart:";
    public static final Long CART_TTL = 360L;
    public static final String ORDER_KEY = "KnowledgeLink:trade:order:";
    public static final Long ORDER_TTL = 30L;
}
