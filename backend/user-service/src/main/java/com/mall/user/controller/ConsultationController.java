package com.mall.user.controller;

import com.mall.user.common.Result;
import com.mall.user.entity.Consultation;
import com.mall.user.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/consultation")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    @GetMapping("/list")
    public Result<List<Consultation>> list(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return Result.success(consultationService.listByUserId(userId));
    }

    @PostMapping("/add")
    public Result<Void> add(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(jwt.getSubject());
        Long productId = body.get("productId") != null ? Long.valueOf(body.get("productId").toString()) : null;
        String question = (String) body.get("question");
        consultationService.add(userId, productId, question);
        return Result.success(null);
    }

    // ─── Admin endpoints ───

    @GetMapping("/admin/list")
    public Result<List<Consultation>> adminList() {
        return Result.success(consultationService.adminList());
    }

    @PutMapping("/admin/reply")
    public Result<Void> adminReply(@RequestParam Long id, @RequestParam String answer) {
        consultationService.reply(id, answer);
        return Result.success(null);
    }
}
