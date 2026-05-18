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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Transactional
    public Result<Void> register(RegisterRequest req) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername())) > 0) {
            return Result.error(400, "用户名已存在");
        }
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

        Instant now = Instant.now();
        Instant accessExp = now.plusSeconds(1800);
        Instant refreshExp = now.plusSeconds(604800);

        String accessToken = generateJwt(user, now, accessExp, "access");
        String refreshToken = generateJwt(user, now, refreshExp, "refresh");

        return Result.success(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(1800)
                .tokenType("Bearer")
                .build());
    }

    private String generateJwt(User user, Instant now, Instant expiration, String type) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://auth-service:8081")
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .claim("type", type)
                .issuedAt(now)
                .expiresAt(expiration)
                .id(UUID.randomUUID().toString())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public Result<Void> forgotPassword(String email) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            return Result.error(400, "该邮箱未注册");
        }
        return Result.success(null);
    }

    public void logout(String refreshToken) {
        redisTemplate.opsForValue().set("token:blacklist:" + refreshToken, "1", Duration.ofDays(7));
    }

    public boolean isTokenBlacklisted(String refreshToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("token:blacklist:" + refreshToken));
    }

    public Result<Long> validateToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return Result.success(Long.valueOf(jwt.getSubject()));
        } catch (Exception e) {
            return Result.error(401, "Invalid token");
        }
    }
}
