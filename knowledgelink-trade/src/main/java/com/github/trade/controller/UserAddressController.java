package com.github.trade.controller;

import com.github.common.dto.Result;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.UserAddressAddDTO;
import com.github.trade.dto.UserAddressUpdateDTO;
import com.github.trade.service.IUserAddressService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/address")
public class UserAddressController {
    private final IUserAddressService userAddressService;

    public UserAddressController(IUserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    @PostMapping("/add")
    public Result addUserAddress(@RequestBody UserAddressAddDTO userAddressAddDTO) {
        return userAddressService.addUserAddress(userAddressAddDTO);
    }

    @PostMapping("/setDefault")
    public Result setDefaultAddress(@RequestBody IdRequest idRequest) {
        return userAddressService.setDefaultAddress(idRequest);
    }

    @GetMapping("/list")
    public Result listUserAddress() {
        return userAddressService.listUserAddress();
    }

    @PostMapping("/update")
    public Result updateUserAddress(@RequestBody UserAddressUpdateDTO userAddressUpdateDTO) {
        return userAddressService.updateUserAddress(userAddressUpdateDTO);
    }

    @DeleteMapping("/delete")
    public Result deleteUserAddress(@RequestBody IdRequest idRequest) {
        return userAddressService.deleteUserAddress(idRequest);
    }
}
