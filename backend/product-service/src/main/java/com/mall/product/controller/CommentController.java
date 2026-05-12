package com.mall.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.common.Result;
import com.mall.product.entity.Comment;
import com.mall.product.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/comment/list/{productId}")
    public Result<IPage<Comment>> list(@PathVariable Long productId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.listByProductId(productId, page, size));
    }

    @PostMapping("/comment/add")
    public Result<Void> add(@AuthenticationPrincipal Jwt jwt,
                             @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(jwt.getSubject());
        Long productId = Long.valueOf(body.get("productId").toString());
        String content = (String) body.get("content");
        Integer rating = Integer.valueOf(body.get("rating").toString());
        String imageUrls = (String) body.getOrDefault("imageUrls", "");
        commentService.add(productId, userId, content, rating, imageUrls);
        return Result.success(null);
    }
}
