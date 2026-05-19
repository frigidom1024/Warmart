package com.mall.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.common.Result;
import com.mall.product.dto.FavoriteVO;
import com.mall.product.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/favorite/add")
    public Result<Void> add(@AuthenticationPrincipal Jwt jwt, @RequestParam Long productId) {
        Long userId = Long.valueOf(jwt.getSubject());
        favoriteService.add(userId, productId);
        return Result.success(null);
    }

    @DeleteMapping("/favorite/cancel/{productId}")
    public Result<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        Long userId = Long.valueOf(jwt.getSubject());
        favoriteService.cancel(userId, productId);
        return Result.success(null);
    }

    @GetMapping("/favorite/list")
    public Result<IPage<FavoriteVO>> list(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Long userId = Long.valueOf(jwt.getSubject());
        return Result.success(favoriteService.listByUserId(userId, page, size));
    }
}
