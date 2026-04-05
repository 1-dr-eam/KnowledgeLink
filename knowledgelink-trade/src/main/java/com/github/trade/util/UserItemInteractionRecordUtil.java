package com.github.trade.util;

import com.github.trade.entity.UserItemInteraction;
import com.github.trade.mapper.UserItemInteractionMapper;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Component
public class UserItemInteractionRecordUtil {
    private final UserItemInteractionMapper userItemInteractionMapper;
    private final ItemInteractionBackfillUtil itemInteractionBackfillUtil;

    public UserItemInteractionRecordUtil(UserItemInteractionMapper userItemInteractionMapper, ItemInteractionBackfillUtil itemInteractionBackfillUtil) {
        this.userItemInteractionMapper = userItemInteractionMapper;
        this.itemInteractionBackfillUtil = itemInteractionBackfillUtil;
    }

    public void recordView(Long userId, Long itemId) {
        recordInteraction(userId, itemId, true, false, false, false);
    }

    public void recordCart(Long userId, Long itemId) {
        recordInteraction(userId, itemId, false, true, false, false);
    }

    public void recordBuy(Long userId, Long itemId) {
        recordInteraction(userId, itemId, false, false, false, true);
    }

    public void recordForward(Long userId, Long itemId) {
        recordInteraction(userId, itemId, false, false, true, false);
    }

    private void recordInteraction(Long userId, Long itemId, boolean click, boolean cart, boolean forward, boolean buy) {
        if (userId == null || itemId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UserItemInteraction interaction = new UserItemInteraction();
        interaction.setUserId(userId);
        interaction.setItemId(itemId);
        interaction.setDateTime(now);
        interaction.setHour(now.getHour());
        interaction.setWeekend(now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY);
        interaction.setHoliday(Boolean.FALSE);
        interaction.setClick(click);
        interaction.setCart(cart);
        interaction.setForward(forward);
        interaction.setBuy(buy);
        int rating = 0;
        if (click) {
            rating += 1;
        }
        if (cart) {
            rating += 1;
        }
        if (forward) {
            rating += 1;
        }
        if (buy) {
            rating += 1;
        }
        interaction.setRating(rating);
        userItemInteractionMapper.insert(interaction);
        itemInteractionBackfillUtil.backfillSingleItem(itemId);
    }
}
