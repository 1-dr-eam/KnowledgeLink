package com.github.trade.util;

import java.util.ArrayList;
import java.util.List;

/**
 * trade 模块 ID 转换工具
 * 前后端传输统一使用 String，服务层在入库前转换为 Long。
 */
public final class TradeIdUtil {
    private TradeIdUtil() {
    }

    public static Long parseId(String idText) {
        if (idText == null || idText.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(idText.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static List<Long> parseIds(List<String> idTexts) {
        if (idTexts == null || idTexts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> idList = new ArrayList<>();
        for (String idText : idTexts) {
            Long id = parseId(idText);
            if (id != null) {
                idList.add(id);
            }
        }
        return idList;
    }
}
