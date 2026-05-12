package com.mall.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mall.user.entity.UserAddress;
import com.mall.user.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressMapper userAddressMapper;

    public List<UserAddress> listByUserId(Long userId) {
        return userAddressMapper.selectList(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getCreatedTime));
    }

    @Transactional
    public void add(UserAddress address) {
        address.setCreatedTime(LocalDateTime.now());
        address.setUpdatedTime(LocalDateTime.now());
        // If this is the first address, set as default
        long count = userAddressMapper.selectCount(
                new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, address.getUserId()));
        if (count == 0) {
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }
        userAddressMapper.insert(address);
    }

    @Transactional
    public void update(UserAddress address) {
        address.setUpdatedTime(LocalDateTime.now());
        userAddressMapper.updateById(address);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        userAddressMapper.delete(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getId, id)
                        .eq(UserAddress::getUserId, userId));
    }

    @Transactional
    public void setDefault(Long id, Long userId) {
        // Unset all defaults for this user
        userAddressMapper.update(null,
                new LambdaUpdateWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .set(UserAddress::getIsDefault, 0));
        // Set this one as default
        userAddressMapper.update(null,
                new LambdaUpdateWrapper<UserAddress>()
                        .eq(UserAddress::getId, id)
                        .eq(UserAddress::getUserId, userId)
                        .set(UserAddress::getIsDefault, 1));
    }
}
