package com.mall.user.controller;

import com.mall.user.common.Result;
import com.mall.user.entity.UserAddress;
import com.mall.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
public class AddressController {

    private final UserAddressService addressService;

    @GetMapping("/list")
    public Result<List<UserAddress>> list(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return Result.success(addressService.listByUserId(userId));
    }

    @PostMapping("/add")
    public Result<Void> add(@AuthenticationPrincipal Jwt jwt, @RequestBody UserAddress address) {
        address.setUserId(Long.valueOf(jwt.getSubject()));
        addressService.add(address);
        return Result.success(null);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody UserAddress address) {
        addressService.update(address);
        return Result.success(null);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        addressService.delete(id, Long.valueOf(jwt.getSubject()));
        return Result.success(null);
    }

    @PutMapping("/default/{id}")
    public Result<Void> setDefault(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        addressService.setDefault(id, Long.valueOf(jwt.getSubject()));
        return Result.success(null);
    }
}
