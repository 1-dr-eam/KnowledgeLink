package com.github.forum.service;

import com.github.common.dto.Result;

public interface IForumShoppingAddressService {
    Result getShoppingAddressByUserId();
    Result addShoppingAddress(String address, String label, Boolean defaultFlag);
    Result deleteShoppingAddressById(Long id);
    Result updateShoppingAddressById(Long id, String address, String label, Boolean defaultFlag);
}
