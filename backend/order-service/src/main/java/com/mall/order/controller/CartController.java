package com.mall.order.controller;

import com.mall.order.common.Result;
import com.mall.order.dto.CartVO;
import com.mall.order.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order/cart")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final CartService cartService;

    @GetMapping("/list")
    public Result<List<CartVO>> list(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return Result.success(cartService.listByUserId(userId));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody AddRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        cartService.add(userId, request.getProductId(), request.getQuantity(), request.getSpecInfo());
        return Result.success(null);
    }

    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody UpdateRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        cartService.updateQuantity(request.getId(), userId, request.getQuantity());
        return Result.success(null);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        cartService.delete(id, userId);
        return Result.success(null);
    }

    @PutMapping("/check")
    public Result<Void> check(@Valid @RequestBody CheckRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        cartService.check(request.getId(), userId, request.getChecked());
        return Result.success(null);
    }

    @PutMapping("/checkAll")
    public Result<Void> checkAll(@Valid @RequestBody CheckAllRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        cartService.checkAll(userId, request.getChecked());
        return Result.success(null);
    }

    @Data
    public static class AddRequest {
        @NotNull
        private Long productId;
        @NotNull
        private Integer quantity;
        private String specInfo;
    }

    @Data
    public static class UpdateRequest {
        @NotNull
        private Long id;
        @NotNull
        private Integer quantity;
    }

    @Data
    public static class CheckRequest {
        @NotNull
        private Long id;
        @NotNull
        private Integer checked;
    }

    @Data
    public static class CheckAllRequest {
        @NotNull
        private Integer checked;
    }
}
