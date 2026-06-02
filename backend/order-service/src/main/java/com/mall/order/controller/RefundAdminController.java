package com.mall.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.order.common.Result;
import com.mall.order.entity.RefundApplication;
import com.mall.order.service.RefundService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order/admin/refund")
@RequiredArgsConstructor
public class RefundAdminController {

    private final RefundService refundService;

    @GetMapping("/list")
    public Result<IPage<RefundApplication>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(refundService.list(status, page, size));
    }

    @PostMapping("/approve")
    public Result<Void> approve(@RequestBody RefundActionRequest request) {
        refundService.approve(request.getId(), request.getAdminReply(), 0L);
        return Result.success(null);
    }

    @PostMapping("/reject")
    public Result<Void> reject(@RequestBody RefundActionRequest request) {
        refundService.reject(request.getId(), request.getAdminReply(), 0L);
        return Result.success(null);
    }

    @Data
    public static class RefundActionRequest {
        private Long id;
        private String adminReply;
    }
}
