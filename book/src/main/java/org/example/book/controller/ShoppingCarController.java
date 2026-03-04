package org.example.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import learning_exchange_platform.model.User;
import org.example.book.entity.Result;
import org.example.book.entity.ShoppingCar;
import org.example.book.service.ShoppingCarService;
import org.example.book.util.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shopping-cart")
public class ShoppingCarController {

    @Autowired
    private ShoppingCarService shoppingCarService;

    @Autowired
    private SessionManager sessionManager;

    @Operation(summary = "添加到购物车")
    @PostMapping("/items")
    public Result addToCart(@RequestParam Integer bookId) {
        try {
            // 从session中获取当前用户
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                return Result.error("用户未登录");
            }

            boolean result = shoppingCarService.addToShoppingCar(currentUser.getId(), bookId);
            if (result) {
                return Result.success("添加到购物车成功");
            } else {
                return Result.error("添加到购物车失败");
            }
        } catch (Exception e) {
            return Result.error("添加到购物车失败: " + e.getMessage());
        }
    }

    @Operation(summary = "从购物车删除")
    @DeleteMapping("/items")
    public Result removeFromCart(@RequestParam Integer bookId) {
        try {
            // 从session中获取当前用户
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                return Result.error("用户未登录");
            }

            boolean result = shoppingCarService.removeFromShoppingCar(currentUser.getId(), bookId);
            if (result) {
                return Result.success("从购物车删除成功");
            } else {
                return Result.error("从购物车删除失败");
            }
        } catch (Exception e) {
            return Result.error("从购物车删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据ID删除购物车商品")
    @DeleteMapping("/items/{id}")
    public Result removeFromCartById(@PathVariable Integer id) {
        try {
            boolean result = shoppingCarService.removeFromShoppingCarById(id);
            if (result) {
                return Result.success("从购物车删除成功");
            } else {
                return Result.error("从购物车删除失败");
            }
        } catch (Exception e) {
            return Result.error("从购物车删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "购买购物车商品")
    @PostMapping("/purchase")
    public Result purchaseFromCart(@RequestBody List<Integer> shoppingCarIds) {
        try {
            // 从session中获取当前用户
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                return Result.error("用户未登录");
            }

            boolean result = shoppingCarService.purchaseFromShoppingCar(currentUser.getId(), shoppingCarIds);
            if (result) {
                return Result.success("购买成功");
            } else {
                return Result.error("购买失败");
            }
        } catch (Exception e) {
            return Result.error("购买失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取购物车")
    @GetMapping("/{userId}")
    public Result getCart(@PathVariable Integer userId) {
        try {
            // 验证当前用户是否与userId匹配 - 手动验证
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null || !currentUser.getId().equals(userId)) {
                return Result.error("无权查看其他用户的购物车");
            }

            List<ShoppingCar> shoppingCar = shoppingCarService.getShoppingCarByUserId(userId);
            return Result.success(shoppingCar);
        } catch (Exception e) {
            return Result.error("获取购物车失败: " + e.getMessage());
        }
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public Result clearCart() {
        try {
            // 从session中获取当前用户
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                return Result.error("用户未登录");
            }

            boolean result = shoppingCarService.clearShoppingCar(currentUser.getId());
            if (result) {
                return Result.success("清空购物车成功");
            } else {
                return Result.error("清空购物车失败");
            }
        } catch (Exception e) {
            return Result.error("清空购物车失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取当前用户的购物车")
    @GetMapping("/my-cart")
    public Result getMyCart() {
        try {
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null) {
                return Result.error("用户信息获取失败");
            }

            List<ShoppingCar> shoppingCar = shoppingCarService.getShoppingCarByUserId(currentUser.getId());
            return Result.success(shoppingCar);
        } catch (Exception e) {
            return Result.error("获取购物车失败: " + e.getMessage());
        }
    }

    // 移除不存在的updateQuantity方法
    // @Operation(summary = "修改购物车商品数量")
    // @PutMapping("/items/{id}/quantity")
    // public Result updateQuantity(@PathVariable Integer id, @RequestParam Integer quantity) {
    //     // 由于ShoppingCar实体类没有quantity字段，移除此方法
    // }

    // 移除不存在的getCartItemCount方法
    // @Operation(summary = "获取购物车商品数量")
    // @GetMapping("/{userId}/count")
    // public Result getCartItemCount(@PathVariable Integer userId) {
    //     // 由于ShoppingCarService没有getCartItemCount方法，移除此方法
    // }

    // 移除不存在的getMyCartItemCount方法
    // @Operation(summary = "获取当前用户购物车商品数量")
    // @GetMapping("/my-cart/count")
    // public Result getMyCartItemCount() {
    //     // 由于ShoppingCarService没有getCartItemCount方法，移除此方法
    // }
}