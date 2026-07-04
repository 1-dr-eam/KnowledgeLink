package com.github.trade.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.Result;
import com.github.common.utils.UserHolder;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.UserAddressAddDTO;
import com.github.trade.dto.UserAddressUpdateDTO;
import com.github.trade.entity.UserAddress;
import com.github.trade.mapper.UserAddressMapper;
import com.github.trade.service.IUserAddressService;
import com.github.trade.util.TradeIdUtil;
import com.github.trade.vo.UserAddressVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements IUserAddressService {
    @Override
    public Result addUserAddress(UserAddressAddDTO userAddressAddDTO) {
        if (!validAddressInput(userAddressAddDTO)) {
            return Result.error("地址参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        Long addressCount = baseMapper.selectCount(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId));
        boolean hasAddress = addressCount != null && addressCount > 0;
        boolean defaultStatus = Boolean.TRUE.equals(userAddressAddDTO.getIsDefault()) || !hasAddress;
        if (defaultStatus) {
            clearDefaultAddress(userId);
        }
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        userAddress.setReceiverName(userAddressAddDTO.getReceiverName());
        userAddress.setReceiverPhone(userAddressAddDTO.getReceiverPhone());
        userAddress.setProvince(userAddressAddDTO.getProvince());
        userAddress.setCity(userAddressAddDTO.getCity());
        userAddress.setDistrict(userAddressAddDTO.getDistrict());
        userAddress.setDetailAddress(userAddressAddDTO.getDetailAddress());
        userAddress.setIsDefault(defaultStatus);
        baseMapper.insert(userAddress);
        return Result.success(userAddress.getId());
    }

    @Override
    public Result setDefaultAddress(IdRequest idRequest) {
        Long addressId = idRequest == null ? null : TradeIdUtil.parseId(idRequest.getId());
        if (addressId == null) {
            return Result.error("地址参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        UserAddress userAddress = baseMapper.selectById(addressId);
        if (userAddress == null || !userId.equals(userAddress.getUserId())) {
            return Result.error("地址不存在");
        }
        if (Boolean.TRUE.equals(userAddress.getIsDefault())) {
            return Result.success();
        }
        clearDefaultAddress(userId);
        userAddress.setIsDefault(Boolean.TRUE);
        baseMapper.updateById(userAddress);
        return Result.success();
    }

    @Override
    public Result listUserAddress() {
        Long userId = UserHolder.getUser().getId();
        List<UserAddress> userAddressList = baseMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdateTime));
        List<UserAddressVO> userAddressVOList = new ArrayList<>();
        for (UserAddress userAddress : userAddressList) {
            userAddressVOList.add(BeanUtil.copyProperties(userAddress, UserAddressVO.class));
        }
        return Result.success(userAddressVOList);
    }

    @Override
    public Result updateUserAddress(UserAddressUpdateDTO userAddressUpdateDTO) {
        Long addressId = userAddressUpdateDTO == null ? null : TradeIdUtil.parseId(userAddressUpdateDTO.getId());
        if (addressId == null) {
            return Result.error("地址参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        UserAddress userAddress = baseMapper.selectById(addressId);
        if (userAddress == null || !userId.equals(userAddress.getUserId())) {
            return Result.error("地址不存在");
        }
        if (StringUtils.hasText(userAddressUpdateDTO.getReceiverName())) {
            userAddress.setReceiverName(userAddressUpdateDTO.getReceiverName());
        }
        if (StringUtils.hasText(userAddressUpdateDTO.getReceiverPhone())) {
            userAddress.setReceiverPhone(userAddressUpdateDTO.getReceiverPhone());
        }
        if (StringUtils.hasText(userAddressUpdateDTO.getProvince())) {
            userAddress.setProvince(userAddressUpdateDTO.getProvince());
        }
        if (StringUtils.hasText(userAddressUpdateDTO.getCity())) {
            userAddress.setCity(userAddressUpdateDTO.getCity());
        }
        if (StringUtils.hasText(userAddressUpdateDTO.getDistrict())) {
            userAddress.setDistrict(userAddressUpdateDTO.getDistrict());
        }
        if (StringUtils.hasText(userAddressUpdateDTO.getDetailAddress())) {
            userAddress.setDetailAddress(userAddressUpdateDTO.getDetailAddress());
        }
        baseMapper.updateById(userAddress);
        return Result.success();
    }

    @Override
    public Result deleteUserAddress(IdRequest idRequest) {
        Long addressId = idRequest == null ? null : TradeIdUtil.parseId(idRequest.getId());
        if (addressId == null) {
            return Result.error("地址参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        UserAddress userAddress = baseMapper.selectById(addressId);
        if (userAddress == null || !userId.equals(userAddress.getUserId())) {
            return Result.error("地址不存在");
        }
        boolean wasDefault = Boolean.TRUE.equals(userAddress.getIsDefault());
        baseMapper.deleteById(addressId);
        if (wasDefault) {
            UserAddress candidate = baseMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getUpdateTime)
                    .last("limit 1"));
            if (candidate != null) {
                candidate.setIsDefault(Boolean.TRUE);
                baseMapper.updateById(candidate);
            }
        }
        return Result.success();
    }

    private boolean validAddressInput(UserAddressAddDTO userAddressAddDTO) {
        if (userAddressAddDTO == null) {
            return false;
        }
        return StringUtils.hasText(userAddressAddDTO.getReceiverName())
                && StringUtils.hasText(userAddressAddDTO.getReceiverPhone())
                && StringUtils.hasText(userAddressAddDTO.getProvince())
                && StringUtils.hasText(userAddressAddDTO.getCity())
                && StringUtils.hasText(userAddressAddDTO.getDistrict())
                && StringUtils.hasText(userAddressAddDTO.getDetailAddress());
    }

    private void clearDefaultAddress(Long userId) {
        baseMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, Boolean.FALSE));
    }
}
