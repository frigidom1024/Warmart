package com.mall.user.controller;

import com.mall.user.common.Result;
import com.mall.user.dto.PasswordRequest;
import com.mall.user.dto.UserInfoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final RestTemplate restTemplate;

    @GetMapping("/info")
    public Result<?> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        try {
            String url = "http://auth-service:8081/api/auth/user/" + userId;
            var response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()), Map.class);
            Map<String, Object> authResult = response.getBody();
            if (authResult != null && Integer.valueOf(200).equals(authResult.get("code"))) {
                return Result.success(authResult.get("data"));
            }
        } catch (Exception ignored) {}
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
