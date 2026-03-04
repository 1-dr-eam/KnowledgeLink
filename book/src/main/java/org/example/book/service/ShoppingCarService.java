package org.example.book.service;

import org.example.book.entity.Book;
import org.example.book.entity.ShoppingCar;
import org.example.book.mapper.ShoppingCarMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShoppingCarService {

    @Autowired
    private ShoppingCarMapper shoppingCarMapper;

    @Autowired
    private OrderService orderService;

    @Transactional
    public boolean addToShoppingCar(Integer userId, Integer bookId) {
        try {
            // 检查书籍是否存在且未出售
            Book book = shoppingCarMapper.getBookById(bookId);
            if (book == null) {
                throw new RuntimeException("书籍不存在");
            }

            if (book.getStatus() != 1) {
                throw new RuntimeException("该书籍已被出售或已下架");
            }

            // 检查用户不能购买自己发布的图书
            if (book.getSellerId().equals(userId)) {
                throw new RuntimeException("不能将自己发布的图书加入购物车");
            }

            // 检查购物车中是否已存在该书籍
            if (shoppingCarMapper.existsInShoppingCar(userId, bookId) > 0) {
                throw new RuntimeException("该书籍已在购物车中");
            }

            // 添加到购物车
            ShoppingCar shoppingCar = new ShoppingCar();
            shoppingCar.setUserId(userId);
            shoppingCar.setBookId(bookId);
            shoppingCar.setSellerId(book.getSellerId());
            shoppingCar.setBookName(book.getName());
            shoppingCar.setPrice(book.getPrice());

            int result = shoppingCarMapper.addToShoppingCar(shoppingCar);
            return result > 0;

        } catch (Exception e) {
            throw new RuntimeException("添加到购物车失败: " + e.getMessage());
        }
    }

    
    public boolean removeFromShoppingCar(Integer userId, Integer bookId) {
        try {
            int result = shoppingCarMapper.removeFromShoppingCar(userId, bookId);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("从购物车删除失败: " + e.getMessage());
        }
    }

    
    public boolean removeFromShoppingCarById(Integer id) {
        try {
            int result = shoppingCarMapper.removeFromShoppingCarById(id);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("从购物车删除失败: " + e.getMessage());
        }
    }

    
    @Transactional
    public boolean purchaseFromShoppingCar(Integer userId, List<Integer> shoppingCarIds) {
        try {
            if (shoppingCarIds == null || shoppingCarIds.isEmpty()) {
                throw new RuntimeException("请选择要购买的商品");
            }

            // 获取购物车中的商品
            List<ShoppingCar> shoppingCarItems = shoppingCarMapper.getShoppingCarByUserId(userId);
            List<ShoppingCar> selectedItems = shoppingCarItems.stream()
                    .filter(item -> shoppingCarIds.contains(item.getId()))
                    .collect(Collectors.toList());

            if (selectedItems.isEmpty()) {
                throw new RuntimeException("未找到选中的商品");
            }

            // 预先验证所有商品状态
            for (ShoppingCar item : selectedItems) {
                Book book = shoppingCarMapper.getBookById(item.getBookId());
                if (book == null) {
                    throw new RuntimeException("商品不存在: " + item.getBookName());
                }
                if (book.getStatus() != 1) {
                    throw new RuntimeException("商品已下架或已出售: " + item.getBookName());
                }
                if (book.getSellerId().equals(userId)) {
                    throw new RuntimeException("不能购买自己发布的商品: " + item.getBookName());
                }
            }

            // 逐个购买购物车中的商品
            for (ShoppingCar item : selectedItems) {
                int result = orderService.purchaseBook(item.getBookId(), userId);
                if (result <= 0) {
                    // 根据错误码提供更详细的错误信息
                    String errorMsg = getPurchaseErrorMessage(result, item.getBookName());
                    throw new RuntimeException(errorMsg);
                }

                // 购买成功后从购物车中移除
                shoppingCarMapper.removeFromShoppingCarById(item.getId());
            }

            return true;

        } catch (Exception e) {
            throw new RuntimeException("购买购物车商品失败: " + e.getMessage());
        }
    }

    // 添加错误信息映射方法
    private String getPurchaseErrorMessage(int errorCode, String bookName) {
        switch (errorCode) {
            case -1:
                return "商品不存在: " + bookName;
            case -2:
                return "商品已下架或已出售: " + bookName;
            case -3:
                return "买家不存在";
            case -4:
                return "余额不足，无法购买: " + bookName;
            case -5:
                return "不能购买自己发布的商品: " + bookName;
            default:
                return "购买商品失败: " + bookName;
        }
    }

    
    public List<ShoppingCar> getShoppingCarByUserId(Integer userId) {
        try {
            // 现在直接返回包含完整book信息的购物车列表
            return shoppingCarMapper.getShoppingCarByUserId(userId);
        } catch (Exception e) {
            throw new RuntimeException("获取购物车失败: " + e.getMessage());
        }
    }

    
    public boolean clearShoppingCar(Integer userId) {
        try {
            int result = shoppingCarMapper.clearShoppingCar(userId);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("清空购物车失败: " + e.getMessage());
        }
    }
}