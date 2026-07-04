package com.github.trade.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.trade.entity.ItemInteraction;
import com.github.trade.entity.UserItemInteraction;
import com.github.trade.mapper.ItemInteractionMapper;
import com.github.trade.mapper.UserItemInteractionMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ItemInteractionBackfillUtil {
    private final UserItemInteractionMapper userItemInteractionMapper;
    private final ItemInteractionMapper itemInteractionMapper;

    public ItemInteractionBackfillUtil(UserItemInteractionMapper userItemInteractionMapper, ItemInteractionMapper itemInteractionMapper) {
        this.userItemInteractionMapper = userItemInteractionMapper;
        this.itemInteractionMapper = itemInteractionMapper;
    }

    public void backfillLastThreeMonths() {
        LocalDateTime startTime = LocalDateTime.now().minusMonths(3);
        List<Map<String, Object>> aggregates = userItemInteractionMapper.selectMaps(
                new QueryWrapper<UserItemInteraction>()
                        .select(
                                "item_id as itemId",
                                "sum(case when click = 1 then 1 else 0 end) as clickCount",
                                "sum(case when cart = 1 then 1 else 0 end) as cartCount",
                                "sum(case when buy = 1 then 1 else 0 end) as buyCount",
                                "sum(case when forward = 1 then 1 else 0 end) as forwardCount")
                        .ge("date_time", startTime)
                        .groupBy("item_id")
        );
        for (Map<String, Object> aggregate : aggregates) {
            Long itemId = aggregate.get("itemId") == null ? null : Long.parseLong(String.valueOf(aggregate.get("itemId")));
            if (itemId == null) {
                continue;
            }
            int clickCount = toInt(aggregate.get("clickCount"));
            int cartCount = toInt(aggregate.get("cartCount"));
            int buyCount = toInt(aggregate.get("buyCount"));
            int forwardCount = toInt(aggregate.get("forwardCount"));
            upsertItemInteraction(itemId, clickCount, cartCount, buyCount, forwardCount);
        }
    }

    public void backfillSingleItem(Long itemId) {
        if (itemId == null) {
            return;
        }
        LocalDateTime startTime = LocalDateTime.now().minusMonths(3);
        Map<String, Object> aggregate = userItemInteractionMapper.selectMaps(
                new QueryWrapper<UserItemInteraction>()
                        .select(
                                "item_id as itemId",
                                "sum(case when click = 1 then 1 else 0 end) as clickCount",
                                "sum(case when cart = 1 then 1 else 0 end) as cartCount",
                                "sum(case when buy = 1 then 1 else 0 end) as buyCount",
                                "sum(case when forward = 1 then 1 else 0 end) as forwardCount")
                        .eq("item_id", itemId)
                        .ge("create_time", startTime)
                        .groupBy("item_id")
        ).stream().findFirst().orElse(null);
        if (aggregate == null) {
            upsertItemInteraction(itemId, 0, 0, 0, 0);
            return;
        }
        upsertItemInteraction(
                itemId,
                toInt(aggregate.get("clickCount")),
                toInt(aggregate.get("cartCount")),
                toInt(aggregate.get("buyCount")),
                toInt(aggregate.get("forwardCount"))
        );
    }

    private void upsertItemInteraction(Long itemId, int clickCount, int cartCount, int buyCount, int forwardCount) {
        ItemInteraction itemInteraction = itemInteractionMapper.selectById(itemId);
        if (itemInteraction == null) {
            itemInteraction = new ItemInteraction();
            itemInteraction.setItemId(itemId);
            itemInteraction.setItemClickLast3M(clickCount);
            itemInteraction.setItemCartLast3M(cartCount);
            itemInteraction.setItemBuyLast3M(buyCount);
            itemInteraction.setItemForwardLast3M(forwardCount);
            itemInteractionMapper.insert(itemInteraction);
            return;
        }
        itemInteraction.setItemClickLast3M(clickCount);
        itemInteraction.setItemCartLast3M(cartCount);
        itemInteraction.setItemBuyLast3M(buyCount);
        itemInteraction.setItemForwardLast3M(forwardCount);
        itemInteractionMapper.updateById(itemInteraction);
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
