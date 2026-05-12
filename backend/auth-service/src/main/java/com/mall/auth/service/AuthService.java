package com.mall.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.auth.common.Result;
import com.mall.auth.dto.AuthResponse;
import com.mall.auth.dto.LoginRequest;
import com.mall.auth.dto.RegisterRequest;
import com.mall.auth.entity.User;
import com.mall.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public Result<Void> register(RegisterRequest req) {
        // Check username uniqueness
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername())) > 0) {
            return Result.error(400, "用户名已存在");
        }
        // Check email uniqueness
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail())) > 0) {
            return Result.error(400, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setRole("USER");
        user.setStatus(0);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success(null);
    }

    public Result<AuthResponse> login(LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        if (user.getStatus() == 1) {
            return Result.error(403, "账号已被禁用");
        }

        // For this initial implementation, generate a simple token
        // In production, this would use OAuth2 authorization server token endpoint
        String accessToken = UUID.randomUUID().toString().replace("-", "");
        String refreshToken = UUID.randomUUID().toString().replace("-", "");

        // Store tokens in Redis for verification
        redisTemplate.opsForValue().set("token:access:" + accessToken, user.getId().toString(), Duration.ofSeconds(1800));
        redisTemplate.opsForValue().set("token:refresh:" + refreshToken, user.getId().toString(), Duration.ofDays(7));

        return Result.success(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(1800)
                .tokenType("Bearer")
                .build());
    }

    public Result<Void> forgotPassword(String email) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            return Result.error(400, "该邮箱未注册");
        }
        // For now, just return success (simulate sending email)
        return Result.success(null);
    }

    public void logout(String refreshToken) {
        // Blacklist the refresh token
        redisTemplate.opsForValue().set("token:blacklist:" + refreshToken, "1", Duration.ofDays(7));
    }

    public boolean isTokenBlacklisted(String refreshToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("token:blacklist:" + refreshToken));
    }

    public Result<Long> validateToken(String token) {
        String userId = (String) redisTemplate.opsForValue().get("token:access:" + token);
        if (userId == null) {
            return Result.error(401, "Invalid or expired token");
        }
        return Result.success(Long.valueOf(userId));
    }
}
