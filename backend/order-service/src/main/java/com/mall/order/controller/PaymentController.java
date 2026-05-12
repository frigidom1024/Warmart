package com.mall.order.controller;

import com.mall.order.common.Result;
import com.mall.order.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order/payment")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay")
    public Result<Void> pay(@Valid @RequestBody PayRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        paymentService.pay(request.getOrderId(), userId, request.getMethod());
        return Result.success(null);
    }

    @Data
    public static class PayRequest {
        @NotNull
        private Long orderId;
        @NotBlank
        private String method;
    }
}
