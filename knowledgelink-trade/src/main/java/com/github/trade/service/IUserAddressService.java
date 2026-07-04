package com.github.trade.service;

import com.github.common.dto.Result;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.UserAddressAddDTO;
import com.github.trade.dto.UserAddressUpdateDTO;

public interface IUserAddressService {
    Result addUserAddress(UserAddressAddDTO userAddressAddDTO);

    Result setDefaultAddress(IdRequest idRequest);

    Result listUserAddress();

    Result updateUserAddress(UserAddressUpdateDTO userAddressUpdateDTO);

    Result deleteUserAddress(IdRequest idRequest);
}
