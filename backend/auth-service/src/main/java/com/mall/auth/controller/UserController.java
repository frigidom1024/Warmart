package com.mall.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.auth.common.Result;
import com.mall.auth.entity.User;
import com.mall.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @GetMapping("/user/search")
    public Result<List<Long>> searchUsersByNickname(@RequestParam String nickname) {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .like(User::getNickname, nickname)
                        .select(User::getId));
        List<Long> ids = users.stream().map(User::getId).collect(Collectors.toList());
        return Result.success(ids);
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
