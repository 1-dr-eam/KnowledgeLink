package com.github.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.common.dto.Result;
import com.github.common.dto.UserDTO;
import com.github.common.utils.UserHolder;
import com.github.forum.entity.ForumShoppingAddress;
import com.github.forum.mapper.ForumShoppingAddressMapper;
import com.github.forum.service.IForumShoppingAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ForumShoppingAddressServiceImpl implements IForumShoppingAddressService {
    @Autowired
    private ForumShoppingAddressMapper forumShoppingAddressMapper;

    @Override
    public Result getShoppingAddressByUserId() {
        Long userId = UserHolder.getUser().getId();
        List<ForumShoppingAddress> addresses = forumShoppingAddressMapper.selectList(new LambdaQueryWrapper<ForumShoppingAddress>()
                .eq(ForumShoppingAddress::getUserId, userId));
        return Result.success(addresses);
    }

    @Override
    public Result addShoppingAddress(String address, String label, Boolean defaultFlag) {
        UserDTO userDTO = UserHolder.getUser();
        ForumShoppingAddress shoppingAddress = new ForumShoppingAddress();
        shoppingAddress.setUserId(userDTO.getId());
        shoppingAddress.setShoppingAddress(address);
        shoppingAddress.setLabel(label);
        shoppingAddress.setDefaultFlag(defaultFlag != null && defaultFlag);
        shoppingAddress.setCreateTime(LocalDateTime.now());
        shoppingAddress.setUpdateTime(LocalDateTime.now());
        forumShoppingAddressMapper.insert(shoppingAddress);
        return Result.success();
    }

    @Override
    public Result deleteShoppingAddressById(Long id) {
        forumShoppingAddressMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result updateShoppingAddressById(Long id, String address, String label, Boolean defaultFlag) {
        forumShoppingAddressMapper.update(null, new LambdaUpdateWrapper<ForumShoppingAddress>()
                .eq(ForumShoppingAddress::getId, id)
                .set(ForumShoppingAddress::getShoppingAddress, address)
                .set(ForumShoppingAddress::getLabel, label)
                .set(ForumShoppingAddress::getDefaultFlag, defaultFlag != null && defaultFlag)
                .set(ForumShoppingAddress::getUpdateTime, LocalDateTime.now()));
        return Result.success();
    }
}
