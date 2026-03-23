package com.github.forum.controller;

import com.github.common.dto.Result;
import com.github.forum.service.IForumShoppingAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShoppingAddressController {
    @Autowired
    private IForumShoppingAddressService forumShoppingAddressService;

    @RequestMapping("/getShoppingAddressByUserId")
    public Result getShoppingAddressByUserId() {
        return forumShoppingAddressService.getShoppingAddressByUserId();
    }

    @RequestMapping("/addShoppingAddress")
    public Result addShoppingAddress(String shoppingAddress, String label, Boolean defaultFlag) {
        return forumShoppingAddressService.addShoppingAddress(shoppingAddress, label, defaultFlag);
    }

    @RequestMapping("/deleteShoppingAddressById")
    public Result deleteShoppingAddressById(Long id) {
        return forumShoppingAddressService.deleteShoppingAddressById(id);
    }

    @RequestMapping("/updateShoppingAddressById")
    public Result updateShoppingAddressById(Long id, String shoppingAddress, String label, Boolean defaultFlag) {
        return forumShoppingAddressService.updateShoppingAddressById(id, shoppingAddress, label, defaultFlag);
    }
}
