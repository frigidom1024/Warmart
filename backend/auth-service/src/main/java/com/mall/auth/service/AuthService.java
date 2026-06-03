package com.mall.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.auth.config.JwtProperties;
import com.mall.auth.common.Result;
import com.mall.auth.dto.*;
import com.mall.auth.service.email.EmailSender;
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
    private final JwtProperties jwtProperties;
    private final EmailSender emailSender;

    @Transactional
    public Result<Void> register(RegisterRequest req) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername())) > 0) {
            return Result.error(400, "用户名已存在");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail())) > 0) {
            return Result.error(400, "邮箱已被注册");
        }

        // 验证注册验证码
        String codeKey = "auth:code:register:" + req.getEmail();
        String cachedCode = (String) redisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            return Result.error(410, "验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(req.getCode())) {
            return Result.error(400, "验证码不正确");
        }
        redisTemplate.delete(codeKey);

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setEmailVerified(true);
        user.setRole("USER");
        user.setStatus(0);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success(null);
    }

    public Result<Boolean> checkEmailRegistered(String email) {
        if (email == null || email.isBlank()) {
            return Result.error(400, "邮箱不能为空");
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email.trim()));
        return Result.success(user != null);
    }

    public Result<Void> sendRegisterCode(SendCodeRequest req) {
        return sendCode(req.getEmail(), "register");
    }

    public Result<Void> sendResetCode(SendCodeRequest req) {
        // 先检查邮箱是否已注册
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()));
        if (user == null) {
            return Result.error(404, "该邮箱未注册");
        }
        return sendCode(req.getEmail(), "reset");
    }

    private Result<Void> sendCode(String email, String type) {
        // 检查频率限制（同一邮箱60秒内不可重复发送）
        String limitKey = "auth:code:limit:" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            return Result.error(429, "发送过于频繁，请60秒后重试");
        }

        // 检查每日发送上限
        String dailyKey = "auth:code:daily:" + email;
        Integer dailyCount = (Integer) redisTemplate.opsForValue().get(dailyKey);
        if (dailyCount != null && dailyCount >= 10) {
            return Result.error(429, "今日验证码发送已达上限");
        }

        // 生成6位随机验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        // 存入 Redis（5分钟过期）
        String codeKey = "auth:code:" + type + ":" + email;
        redisTemplate.opsForValue().set(codeKey, code, Duration.ofSeconds(300));

        // 设置频率限制（60秒过期）
        redisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(60));

        // 更新每日计数
        if (dailyCount == null) {
            redisTemplate.opsForValue().set(dailyKey, 1, Duration.ofDays(1));
        } else {
            redisTemplate.opsForValue().increment(dailyKey);
        }

        // 发送邮件
        String subject = type.equals("register") ? "注册验证码" : "密码重置验证码";
        String content = String.format(
                "您的%s为：%s，有效期5分钟，请勿泄露给他人。",
                type.equals("register") ? "注册验证码" : "密码重置验证码", code);
        emailSender.send(email, subject, content);

        return Result.success(null);
    }

    public Result<Void> verifyCode(VerifyCodeRequest req) {
        String codeKey = "auth:code:" + req.getType() + ":" + req.getEmail();
        String cachedCode = (String) redisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            return Result.error(410, "验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(req.getCode())) {
            return Result.error(400, "验证码不正确");
        }
        // 验证通过后删除（一次性使用）
        redisTemplate.delete(codeKey);
        return Result.success(null);
    }

    public Result<Void> resetPassword(ResetPasswordRequest req) {
        // 先验证验证码
        String codeKey = "auth:code:reset:" + req.getEmail();
        String cachedCode = (String) redisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            return Result.error(410, "验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(req.getCode())) {
            return Result.error(400, "验证码不正确");
        }
        redisTemplate.delete(codeKey);

        // 查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()));
        if (user == null) {
            return Result.error(404, "该邮箱未注册");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);

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
        Instant accessExp = now.plusSeconds(jwtProperties.getAccessTokenExp());
        Instant refreshExp = now.plusSeconds(jwtProperties.getRefreshTokenExp());

        String accessToken = generateJwt(user, now, accessExp, "access");
        String refreshToken = generateJwt(user, now, refreshExp, "refresh");

        return Result.success(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn((int) jwtProperties.getAccessTokenExp())
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

    public Result<AuthResponse> refresh(String refreshToken) {
        try {
            Jwt jwt = jwtDecoder.decode(refreshToken);
            if (!"refresh".equals(jwt.getClaimAsString("type"))) {
                return Result.error(400, "Invalid refresh token");
            }
            if (isTokenBlacklisted(refreshToken)) {
                return Result.error(401, "Refresh token has been revoked");
            }
            Long userId = Long.valueOf(jwt.getSubject());
            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error(401, "User not found");
            }
            if (user.getStatus() == 1) {
                return Result.error(403, "Account disabled");
            }

            Instant now = Instant.now();
            Instant accessExp = now.plusSeconds(jwtProperties.getAccessTokenExp());
            Instant refreshExp = now.plusSeconds(jwtProperties.getRefreshTokenExp());

            String newAccessToken = generateJwt(user, now, accessExp, "access");
            String newRefreshToken = generateJwt(user, now, refreshExp, "refresh");

            return Result.success(AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn((int) jwtProperties.getAccessTokenExp())
                    .tokenType("Bearer")
                    .build());
        } catch (Exception e) {
            return Result.error(401, "Invalid refresh token");
        }
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
