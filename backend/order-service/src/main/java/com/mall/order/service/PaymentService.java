package com.mall.order.service;

import com.mall.order.entity.Order;
import com.mall.order.entity.Payment;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    @Transactional
    public void pay(Long orderId, Long userId, String method) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) {
            throw new RuntimeException("Order cannot be paid");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("Order not found");
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setOrderNo(order.getOrderNo());
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(method);
        payment.setStatus(1); // paid
        payment.setPayTime(LocalDateTime.now());
        paymentMapper.insert(payment);

        // Update order status
        order.setStatus(1); // pending_delivery
        order.setPaymentMethod(method);
        order.setPaymentTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional
    public void refund(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 5) {
            throw new RuntimeException("Order cannot be refunded");
        }

        // Update payment record
        Payment payment = paymentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId));
        if (payment != null) {
            payment.setStatus(2); // refunded
            paymentMapper.updateById(payment);
        }

        // Update order status
        order.setStatus(4); // cancelled (refunded)
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }
}
