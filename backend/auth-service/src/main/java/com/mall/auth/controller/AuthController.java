package com.mall.auth.controller;

import com.mall.auth.common.Result;
import com.mall.auth.dto.*;
import com.mall.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody SendCodeRequest req) {
        return authService.sendResetCode(req);
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@RequestParam String refreshToken) {
        return authService.refresh(refreshToken);
    }

    // ---- 新增验证码端点 ----

    @PostMapping("/code/send-register")
    public Result<Void> sendRegisterCode(@Valid @RequestBody SendCodeRequest req) {
        return authService.sendRegisterCode(req);
    }

    @PostMapping("/code/send-reset")
    public Result<Void> sendResetCode(@Valid @RequestBody SendCodeRequest req) {
        return authService.sendResetCode(req);
    }

    @PostMapping("/code/verify")
    public Result<Void> verifyCode(@Valid @RequestBody VerifyCodeRequest req) {
        return authService.verifyCode(req);
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return authService.resetPassword(req);
    }

    @GetMapping("/check-email")
    public Result<Boolean> checkEmail(@RequestParam String email) {
        return authService.checkEmailRegistered(email);
    }
}
