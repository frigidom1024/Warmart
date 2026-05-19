package com.mall.auth.controller;

import com.mall.auth.common.Result;
import com.mall.auth.entity.User;
import com.mall.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;

    @GetMapping("/user/{id}")
    public Result<Map<String, Object>> getUserById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "User not found");
        }
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("email", user.getEmail());
        info.put("phone", user.getPhone());
        info.put("avatar", user.getAvatar());
        info.put("role", user.getRole());
        return Result.success(info);
    }

    @GetMapping("/user/batch")
    public Result<Map<Long, Map<String, Object>>> getUsersByIds(@RequestParam String ids) {
        String[] idArr = ids.split(",");
        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (String idStr : idArr) {
            try {
                Long id = Long.valueOf(idStr.trim());
                User user = userMapper.selectById(id);
                if (user != null) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", user.getId());
                    info.put("nickname", user.getNickname());
                    info.put("avatar", user.getAvatar());
                    result.put(id, info);
                }
            } catch (NumberFormatException ignored) {}
        }
        return Result.success(result);
    }
}
