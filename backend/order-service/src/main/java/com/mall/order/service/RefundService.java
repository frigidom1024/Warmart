package com.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.order.entity.Order;
import com.mall.order.entity.RefundApplication;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.RefundApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundApplicationMapper refundApplicationMapper;
    private final OrderMapper orderMapper;

    @Transactional
    public RefundApplication create(Long orderId, Long userId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }
        if (order.getStatus() != 1 && order.getStatus() != 2 && order.getStatus() != 3) {
            throw new RuntimeException("Order cannot be refunded");
        }

        // Create refund application
        RefundApplication app = new RefundApplication();
        app.setOrderId(orderId);
        app.setUserId(userId);
        app.setReason(reason);
        app.setAmount(order.getTotalAmount());
        app.setPreviousStatus(order.getStatus());
        app.setStatus("PENDING");
        app.setCreatedTime(LocalDateTime.now());
        app.setUpdatedTime(LocalDateTime.now());
        refundApplicationMapper.insert(app);

        // Update order status to refunding
        order.setStatus(5);
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.updateById(order);

        return app;
    }

    @Transactional
    public void cancelByOrderId(Long orderId, Long userId) {
        RefundApplication app = refundApplicationMapper.selectOne(
                new LambdaQueryWrapper<RefundApplication>()
                        .eq(RefundApplication::getOrderId, orderId)
                        .eq(RefundApplication::getUserId, userId)
                        .eq(RefundApplication::getStatus, "PENDING"));
        if (app == null) {
            throw new RuntimeException("Refund application not found or already processed");
        }

        app.setStatus("CANCELLED");
        app.setHandledTime(LocalDateTime.now());
        app.setUpdatedTime(LocalDateTime.now());
        refundApplicationMapper.updateById(app);

        // Restore order to previous status
        Order order = orderMapper.selectById(app.getOrderId());
        if (order != null && order.getStatus() == 5) {
            int restoreStatus = app.getPreviousStatus() != null ? app.getPreviousStatus() : 3;
            order.setStatus(restoreStatus);
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    public IPage<RefundApplication> list(String status, int page, int size) {
        LambdaQueryWrapper<RefundApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(RefundApplication::getStatus, status);
        }
        wrapper.orderByDesc(RefundApplication::getCreatedTime);
        IPage<RefundApplication> appPage = refundApplicationMapper.selectPage(new Page<>(page, size), wrapper);

        // Populate order info
        for (RefundApplication app : appPage.getRecords()) {
            Order order = orderMapper.selectById(app.getOrderId());
            if (order != null) {
                app.setOrderNo(order.getOrderNo());
                app.setReceiverName(order.getReceiverName());
            }
        }

        return appPage;
    }

    @Transactional
    public void approve(Long id, String adminReply, Long adminId) {
        RefundApplication app = refundApplicationMapper.selectById(id);
        if (app == null || !"PENDING".equals(app.getStatus())) {
            throw new RuntimeException("Refund application not found or already processed");
        }

        app.setStatus("APPROVED");
        app.setAdminReply(adminReply);
        app.setHandledTime(LocalDateTime.now());
        app.setUpdatedTime(LocalDateTime.now());
        refundApplicationMapper.updateById(app);

        // Set order to cancelled (refunded)
        Order order = orderMapper.selectById(app.getOrderId());
        if (order != null) {
            order.setStatus(4);
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    @Transactional
    public void reject(Long id, String adminReply, Long adminId) {
        RefundApplication app = refundApplicationMapper.selectById(id);
        if (app == null || !"PENDING".equals(app.getStatus())) {
            throw new RuntimeException("Refund application not found or already processed");
        }

        app.setStatus("REJECTED");
        app.setAdminReply(adminReply);
        app.setHandledTime(LocalDateTime.now());
        app.setUpdatedTime(LocalDateTime.now());
        refundApplicationMapper.updateById(app);

        // Restore order to completed
        Order order = orderMapper.selectById(app.getOrderId());
        if (order != null) {
            order.setStatus(3);
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }
}
