package com.mall.user.controller;

import com.mall.user.common.Result;
import com.mall.user.dto.PasswordRequest;
import com.mall.user.dto.UserInfoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/info")
    public Result<?> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        // For now return basic info - will expand with user details
        return Result.success(jwt.getClaims());
    }

    @PutMapping("/info")
    public Result<Void> updateUserInfo(@AuthenticationPrincipal Jwt jwt, @RequestBody UserInfoRequest req) {
        // Placeholder - will implement with user details from auth service
        return Result.success(null);
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@AuthenticationPrincipal Jwt jwt, @RequestBody PasswordRequest req) {
        // Placeholder - will implement password change
        return Result.success(null);
    }
}
