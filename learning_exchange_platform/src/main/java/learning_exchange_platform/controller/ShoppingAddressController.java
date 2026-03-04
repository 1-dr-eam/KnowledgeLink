package learning_exchange_platform.controller;

import jakarta.servlet.http.HttpSession;
import learning_exchange_platform.mapper.ShoppingAddressMapper;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.model.ShoppingAddress;
import learning_exchange_platform.utils.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ShoppingAddressController {

    @Autowired
    private ShoppingAddressMapper shoppingAddressMapper;

    @RequestMapping("/getShoppingAddressByUserId")
    public Result getShoppingAddressByUserId() {
        try {
            HttpSession session= SessionUtil.getSession();
            int user_id=(int)session.getAttribute("user_id");
            List<ShoppingAddress> address= shoppingAddressMapper.selectShoppingAddressByUserId(user_id);
            return Result.success(address);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    @RequestMapping("/addShoppingAddress")
    public Result addShoppingAddress(String address,String label,boolean default_flag) {
        try {
            HttpSession session= SessionUtil.getSession();
            int user_id=(int)session.getAttribute("user_id");
            boolean success= shoppingAddressMapper.insertShoppingAddress(user_id,address,label,default_flag);
            if(success) {
                return Result.success();
            }else {
                return Result.error("添加收货地址失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    @RequestMapping("/deleteShoppingAddressById")
    public Result deleteShoppingAddressById(int id) {
        try {
            boolean success= shoppingAddressMapper.deleteShoppingAddressById(id);
            if(success) {
                return Result.success();
            }else {
                return Result.error("删除收货地址失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    @RequestMapping("/updateShoppingAddressById")
    public Result updateShoppingAddressById(int id,String address,String label,boolean default_flag) {
        try {
            boolean success= shoppingAddressMapper.updateShoppingAddress(id,address,label,default_flag);
            if(success) {
                return Result.success();
            }else {
                return Result.error("更新收货地址失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }
}
