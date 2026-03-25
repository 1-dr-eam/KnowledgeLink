package com.github.common.utils;

import org.springframework.stereotype.Component;

/**
 * id比较工具类 -> 生成 A_B类型的String字符串
 *
 * @author ning
 * @date 2026/03/25
 */
@Component
public class IdCompareUtil {

    /**
     * 返回String类型的 A_B
     *
     * @param firstId  id1
     * @param secondId id2
     * @return String
     */
    public String idCompare(Long firstId, Long secondId) {
        String first = String.valueOf(Math.max(firstId, secondId));
        String second = String.valueOf(Math.min(firstId, secondId));
        return first + "_" + second;
    }

    /**
     * 获取第一个ID
     *
     * @param id ID
     * @return 第一个id
     */
    public Long getFirstId(String id){
        return Long.valueOf(id.split("_")[0]);
    }

    /**
     * 获取第二个ID
     *
     * @param id ID
     * @return 第二个id
     */
    public Long getSecondId(String id){
        return Long.valueOf(id.split("_")[1]);
    }

}
