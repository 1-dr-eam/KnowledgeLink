package com.github.trade.controller;

import com.github.common.dto.Result;
import com.github.trade.dto.BatchIdRequest;
import com.github.trade.dto.CartUpsertRequest;
import com.github.trade.dto.IdRequest;
import com.github.trade.service.ICartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 购物车controller
 *
 * @author ning
 * @date 2026/03/18
 */
@RestController
public class CartController {

    @Autowired
    private ICartService cartService;

    @PostMapping("/insertCartInfo")
    public Result insertCartInfo(@RequestBody CartUpsertRequest cartUpsertRequest) {
        return cartService.insertCartInfo(cartUpsertRequest);
    }

    @PutMapping("/addCartByStep")
    public Result addCartByStep(@RequestBody IdRequest idRequest) {
        return cartService.addCartByStep(idRequest);
    }

    @PutMapping("/reduceCartByStep")
    public Result reduceCartByStep(@RequestBody IdRequest idRequest) {
        return cartService.reduceCartByStep(idRequest);
    }

    @PutMapping("/updateCartByCount")
    public Result updateCartByCount(@RequestBody CartUpsertRequest cartUpsertRequest) {
        return cartService.updateCartByCount(cartUpsertRequest);
    }

    @GetMapping("/getAllCart")
    public Result getAllCart() {
        return cartService.getAllCart();
    }

    @DeleteMapping("/deleteCartById")
    public Result deleteCartById(@RequestBody IdRequest idRequest) {
        return cartService.deleteCartById(idRequest);
    }

    @DeleteMapping("/deleteCartByBatch")
    public Result deleteCartByBatch(@RequestBody BatchIdRequest batchIdRequest) {
        return cartService.deleteCartByBatch(batchIdRequest);
    }
}
