package com.github.common.utils;

/**
 * @author ning
 * @date 2026-03-10
 */
public class RedisConstant {

    // user模块
    public static final String USER_INFO_KEY = "KnowledgeLink:user:info";
    public static final Long USER_INFO_TTL = 86400L;
    public static final String USER_FOLLOW_KEY = "KnowledgeLink:user:follow:";
    public static final Long USER_FOLLOW_TTL = 86400L;

    // trade模块
    public static final String BOOK_INFO_KEY = "KnowledgeLink:trade:bookInfo:";
    public static final Long BOOK_INFO_TTL = 360L;
    public static final String CART_KEY = "KnowledgeLink:trade:cart:";
    public static final Long CART_TTL = 360L;
    public static final String ORDER_KEY = "KnowledgeLink:trade:order:";
    public static final Long ORDER_TTL = 30L;
}
