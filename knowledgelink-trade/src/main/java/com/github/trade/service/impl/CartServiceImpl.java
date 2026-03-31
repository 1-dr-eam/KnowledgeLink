package com.github.trade.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.Result;
import com.github.common.utils.UserHolder;
import com.github.trade.dto.BatchIdRequest;
import com.github.trade.dto.BookDTO;
import com.github.trade.dto.CartUpsertRequest;
import com.github.trade.dto.IdRequest;
import com.github.trade.entity.ShoppingCar;
import com.github.trade.mapper.CartMapper;
import com.github.trade.service.ICartService;
import com.github.trade.vo.CartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.common.utils.RedisConstant.CART_KEY;
import static com.github.common.utils.RedisConstant.CART_TTL;

/**
 * @author ning
 * @date 2026-03-18
 */
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, ShoppingCar> implements ICartService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private BookServiceImpl bookServiceImpl;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 向购物车插入商品
     *
     * @param cartUpsertRequest 购物车upsert请求dto
     * @return success
     */
    @Override
    public Result insertCartInfo(CartUpsertRequest cartUpsertRequest) {
        if (cartUpsertRequest == null || cartUpsertRequest.getId() == null || cartUpsertRequest.getCount() == null || cartUpsertRequest.getCount() <= 0) {
            return Result.error("购物车参数错误");
        }
        BookDTO bookDTO = bookServiceImpl.getBookInfoById(cartUpsertRequest.getId());
        if (bookDTO == null) {
            return Result.error("商品信息不存在");
        }
        if (bookDTO.getCount() == null || bookDTO.getCount() < cartUpsertRequest.getCount()) {
            return Result.error("商品库存数量不足，请重新选择");
        }
        Long userId = UserHolder.getUser().getId();
        ShoppingCar oldShoppingCar = cartMapper.selectOne(
                new LambdaQueryWrapper<ShoppingCar>()
                        .eq(ShoppingCar::getUserId, userId)
                        .eq(ShoppingCar::getBookId, cartUpsertRequest.getId())
        );
        // 重复商品判断
        if (oldShoppingCar != null) {
            int newCount = oldShoppingCar.getCount() + cartUpsertRequest.getCount();
            if (bookDTO.getCount() < newCount) {
                return Result.error("商品库存数量不足，请重新选择");
            }
            oldShoppingCar.setCount(newCount);
            oldShoppingCar.setInventory(true);
            baseMapper.updateById(oldShoppingCar);
            syncCartCountToRedis(userId, oldShoppingCar.getBookId(), newCount);
            return Result.success();
        }
        ShoppingCar shoppingCar = new ShoppingCar();
        shoppingCar.setUserId(userId);
        shoppingCar.setBookId(cartUpsertRequest.getId());
        shoppingCar.setCount(cartUpsertRequest.getCount());
        shoppingCar.setInventory(true);
        baseMapper.insert(shoppingCar);
        syncCartCountToRedis(userId, shoppingCar.getBookId(), shoppingCar.getCount());
        return Result.success();
    }

    @Override
    public Result addCartByStep(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("购物车参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        ShoppingCar shoppingCar = getUserCartById(idRequest.getId(), userId);
        if (shoppingCar == null) {
            return Result.error("购物车记录不存在");
        }
        BookDTO bookDTO = bookServiceImpl.getBookInfoById(shoppingCar.getBookId());
        if (bookDTO == null || bookDTO.getCount() == null) {
            return Result.error("商品信息不存在");
        }
        int newCount = shoppingCar.getCount() + 1;
        if (newCount > bookDTO.getCount()) {
            return Result.error("商品库存数量不足，请重新选择");
        }
        shoppingCar.setCount(newCount);
        shoppingCar.setInventory(true);
        baseMapper.updateById(shoppingCar);
        addCartCountToRedis(userId, shoppingCar.getBookId(), 1L);
        return Result.success();
    }

    @Override
    public Result reduceCartByStep(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("购物车参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        ShoppingCar shoppingCar = getUserCartById(idRequest.getId(), userId);
        if (shoppingCar == null) {
            return Result.error("购物车记录不存在");
        }
        if (shoppingCar.getCount() <= 1) {
            baseMapper.deleteById(shoppingCar.getId());
            syncCartCountToRedis(userId, shoppingCar.getBookId(), 0);
            return Result.success();
        }
        int newCount = shoppingCar.getCount() - 1;
        BookDTO bookDTO = bookServiceImpl.getBookInfoById(shoppingCar.getBookId());
        shoppingCar.setCount(newCount);
        shoppingCar.setInventory(bookDTO != null && bookDTO.getCount() != null && bookDTO.getCount() >= newCount);
        baseMapper.updateById(shoppingCar);
        addCartCountToRedis(userId, shoppingCar.getBookId(), -1L);
        return Result.success();
    }

    @Override
    public Result updateCartByCount(CartUpsertRequest cartUpsertRequest) {
        if (cartUpsertRequest == null || cartUpsertRequest.getId() == null || cartUpsertRequest.getCount() == null || cartUpsertRequest.getCount() <= 0) {
            return Result.error("购物车参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        ShoppingCar shoppingCar = getUserCartById(cartUpsertRequest.getId(), userId);
        if (shoppingCar == null) {
            return Result.error("购物车记录不存在");
        }
        BookDTO bookDTO = bookServiceImpl.getBookInfoById(shoppingCar.getBookId());
        if (bookDTO == null || bookDTO.getCount() == null) {
            return Result.error("商品信息不存在");
        }
        if (cartUpsertRequest.getCount() > bookDTO.getCount()) {
            return Result.error("商品库存数量不足，请重新选择");
        }
        shoppingCar.setCount(cartUpsertRequest.getCount());
        shoppingCar.setInventory(true);
        baseMapper.updateById(shoppingCar);
        syncCartCountToRedis(userId, shoppingCar.getBookId(), shoppingCar.getCount());
        return Result.success();
    }

    /**
     * 获取购物车所有商品信息
     *
     * @return CartVo
     */
    @Override
    public Result getAllCart() {
        Long userId = UserHolder.getUser().getId();
        List<CartVO> cartVOList = new ArrayList<>();
        List<ShoppingCar> cartList = cartMapper.selectList(
                new LambdaQueryWrapper<ShoppingCar>()
                        .eq(ShoppingCar::getUserId, userId)
        );
        for (ShoppingCar shoppingCar : cartList) {
            CartVO cartVO = BeanUtil.copyProperties(shoppingCar, CartVO.class);
            BookDTO bookDTO = bookServiceImpl.getBookInfoById(shoppingCar.getBookId());
            if (bookDTO == null) {
                cartVO.setInventory(false);
            } else {
                cartVO.setBookName(bookDTO.getName());
                cartVO.setBookAuthor(bookDTO.getAuthor());
                cartVO.setBookPublisher(bookDTO.getPublisher());
                cartVO.setBookVersion(bookDTO.getVersion());
                cartVO.setBookPrice(bookDTO.getPrice());
                cartVO.setBookType(bookDTO.getType());
                cartVO.setBookItemCategories(bookDTO.getItemCategories());
                cartVO.setBookItemKeywords(bookDTO.getItemKeywords());
                cartVO.setBookImage(bookDTO.getImage());
                cartVO.setBookStatus(bookDTO.getStatus());
                cartVO.setInventory(bookDTO.getCount() != null && shoppingCar.getCount() != null && bookDTO.getCount() >= shoppingCar.getCount());
                if (shoppingCar.getCount() != null) {
                    cartVO.setAmount(bookDTO.getPrice() * shoppingCar.getCount());
                }
            }
            cartVOList.add(cartVO);
        }
        return Result.success(cartVOList);
    }

    @Override
    public Result deleteCartById(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("购物车参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        ShoppingCar shoppingCar = getUserCartById(idRequest.getId(), userId);
        if (shoppingCar == null) {
            return Result.error("购物车记录不存在");
        }
        baseMapper.deleteById(shoppingCar.getId());
        syncCartCountToRedis(userId, shoppingCar.getBookId(), 0);
        return Result.success();
    }

    @Override
    public Result deleteCartByBatch(BatchIdRequest batchIdRequest) {
        if (batchIdRequest == null || batchIdRequest.getIds() == null || batchIdRequest.getIds().isEmpty()) {
            return Result.error("购物车参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        List<ShoppingCar> cartList = cartMapper.selectList(
                new LambdaQueryWrapper<ShoppingCar>()
                        .eq(ShoppingCar::getUserId, userId)
                        .in(ShoppingCar::getId, batchIdRequest.getIds())
        );
        if (cartList.isEmpty()) {
            return Result.success();
        }
        List<Long> cartIdList = cartList.stream().map(ShoppingCar::getId).toList();
        baseMapper.delete(
                new LambdaQueryWrapper<ShoppingCar>()
                        .eq(ShoppingCar::getUserId, userId)
                        .in(ShoppingCar::getId, cartIdList)
        );
        String tokenKey = CART_KEY + userId;
        Object[] bookIdArray = cartList.stream().map(item -> String.valueOf(item.getBookId())).toArray();
        if (bookIdArray.length > 0) {
            stringRedisTemplate.opsForHash().delete(tokenKey, bookIdArray);
        }
        return Result.success();
    }

    private ShoppingCar getUserCartById(Long cartId, Long userId) {
        return cartMapper.selectOne(
                new LambdaQueryWrapper<ShoppingCar>()
                        .eq(ShoppingCar::getId, cartId)
                        .eq(ShoppingCar::getUserId, userId)
        );
    }

    private void syncCartCountToRedis(Long userId, Long bookId, Integer count) {
        String tokenKey = CART_KEY + userId;
        String bookIdField = String.valueOf(bookId);
        if (count == null || count <= 0) {
            stringRedisTemplate.opsForHash().delete(tokenKey, bookIdField);
            return;
        }
        stringRedisTemplate.opsForHash().put(tokenKey, bookIdField, String.valueOf(count));
        stringRedisTemplate.expire(tokenKey, CART_TTL, TimeUnit.MINUTES);
    }

    private void addCartCountToRedis(Long userId, Long bookId, Long delta) {
        String tokenKey = CART_KEY + userId;
        String bookIdField = String.valueOf(bookId);
        Long count = stringRedisTemplate.opsForHash().increment(tokenKey, bookIdField, delta);
        if (count != null && count <= 0) {
            stringRedisTemplate.opsForHash().delete(tokenKey, bookIdField);
            return;
        }
        stringRedisTemplate.expire(tokenKey, CART_TTL, TimeUnit.MINUTES);
    }
}
