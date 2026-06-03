package com.mall.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.order.common.Result;
import com.alibaba.excel.EasyExcel;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderExportVO;
import com.mall.order.entity.RefundApplication;
import com.mall.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public Result<Order> create(@Valid @RequestBody CreateOrderRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        Order order = orderService.create(userId, request.getReceiverName(), request.getReceiverPhone(),
                request.getReceiverAddress(), request.getPaymentMethod(), request.getCartItemIds());
        return Result.success(order);
    }

    @GetMapping("/list")
    public Result<IPage<Order>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        return Result.success(orderService.listByUserId(userId, status, page, size));
    }

    @GetMapping("/detail/{id}")
    public Result<Order> detail(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Order order = orderService.detail(id);
        if (order == null) {
            return Result.error("Order not found");
        }
        Long userId = Long.valueOf(jwt.getSubject());
        if (!order.getUserId().equals(userId)) {
            return Result.error("Order not found");
        }
        return Result.success(order);
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        orderService.cancel(id, userId);
        return Result.success(null);
    }

    @PostMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        orderService.confirm(id, userId);
        return Result.success(null);
    }

    @PostMapping("/refund/{id}")
    public Result<Void> refund(@PathVariable Long id,
                               @RequestParam String reason,
                               @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        orderService.applyRefund(id, userId, reason);
        return Result.success(null);
    }

    @PostMapping("/refund/cancel/{id}")
    public Result<Void> cancelRefund(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        orderService.cancelRefund(id, userId);
        return Result.success(null);
    }

    @GetMapping("/refund/info/{id}")
    public Result<RefundApplication> refundInfo(@PathVariable Long id) {
        return Result.success(orderService.getRefundInfo(id));
    }

    // ─── Admin endpoints ───

    @GetMapping("/admin/list")
    public Result<IPage<Order>> adminList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) String receiverPhone,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(orderService.adminList(status, orderNo, receiverName, receiverPhone, startTime, endTime, page, size));
    }

    @PutMapping("/admin/status")
    public Result<Void> adminUpdateStatus(@RequestParam Long id, @RequestParam Integer status) {
        orderService.adminUpdateStatus(id, status);
        return Result.success(null);
    }

    @GetMapping("/admin/stats")
    public Result<Map<String, Object>> adminDashboardStats() {
        return Result.success(orderService.getDashboardStats());
    }

    @PutMapping("/admin/ship")
    public Result<Void> adminShip(@RequestParam Long id,
                                  @RequestParam String logisticsCompany,
                                  @RequestParam String logisticsNo) {
        orderService.adminShip(id, logisticsCompany, logisticsNo);
        return Result.success(null);
    }

    @PutMapping("/admin/cancel")
    public Result<Void> adminCancel(@RequestParam Long id) {
        orderService.adminCancel(id);
        return Result.success(null);
    }

    @GetMapping("/admin/export")
    public void adminExport(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) String receiverPhone,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletResponse response) throws IOException {
        List<OrderExportVO> list = orderService.exportList(status, orderNo, receiverName, receiverPhone, startTime, endTime);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode("订单数据", StandardCharsets.UTF_8) + ".xlsx");
        EasyExcel.write(response.getOutputStream(), OrderExportVO.class).sheet("订单数据").doWrite(list);
    }

    @Data
    public static class CreateOrderRequest {
        @NotBlank
        private String receiverName;
        @NotBlank
        private String receiverPhone;
        @NotBlank
        private String receiverAddress;
        @NotBlank
        private String paymentMethod;
        @NotNull
        private List<Long> cartItemIds;
    }
}
