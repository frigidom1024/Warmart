package com.mall.user.controller;

import com.mall.user.common.Result;
import com.mall.user.entity.UserFeedback;
import com.mall.user.service.UserFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final UserFeedbackService feedbackService;

    @PostMapping("/add")
    public Result<Void> add(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, String> body) {
        Long userId = Long.valueOf(jwt.getSubject());
        feedbackService.add(userId, body.get("type"), body.get("content"));
        return Result.success(null);
    }

    // ─── Admin endpoints ───

    @GetMapping("/admin/list")
    public Result<List<UserFeedback>> adminList() {
        return Result.success(feedbackService.adminList());
    }

    @PutMapping("/admin/reply")
    public Result<Void> adminReply(@RequestParam Long id, @RequestParam String replyContent) {
        feedbackService.reply(id, replyContent);
        return Result.success(null);
    }
}
