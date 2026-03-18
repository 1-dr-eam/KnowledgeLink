package com.github.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.common.dto.Result;
import com.github.trade.dto.BatchIdRequest;
import com.github.trade.dto.CartUpsertRequest;
import com.github.trade.dto.IdRequest;
import com.github.trade.entity.ShoppingCar;

/**
 * 购物车service接口
 *
 * @author ning
 * @date 2026/03/18
 */
public interface ICartService extends IService<ShoppingCar> {
    Result insertCartInfo(CartUpsertRequest cartUpsertRequest);

    Result addCartByStep(IdRequest idRequest);

    Result reduceCartByStep(IdRequest idRequest);

    Result updateCartByCount(CartUpsertRequest cartUpsertRequest);

    Result getAllCart();

    Result deleteCartById(IdRequest idRequest);

    Result deleteCartByBatch(BatchIdRequest batchIdRequest);
}
