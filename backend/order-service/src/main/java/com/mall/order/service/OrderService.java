package com.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartService cartService;

    @Transactional
    public Order create(Long userId, String receiverName, String receiverPhone,
                        String receiverAddress, String paymentMethod, List<Long> cartItemIds) {
        // Get checked cart items
        var cartItems = cartService.getCheckedItems(userId);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("No items selected");
        }

        // Generate order no
        String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%08d", new Random().nextInt(100000000));

        // Calculate total
        BigDecimal total = cartItems.stream()
                .map(item -> {
                    // In a real app, fetch price from product-service
                    return BigDecimal.valueOf(100); // placeholder
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create order
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setTotalAmount(total);
        order.setStatus(0); // pending_payment
        order.setPaymentMethod(paymentMethod);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setCreatedTime(LocalDateTime.now());
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.insert(order);

        // Create order items
        for (var cartItem : cartItems) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(cartItem.getProductId());
            item.setProductName("Product " + cartItem.getProductId()); // placeholder
            item.setQuantity(cartItem.getQuantity());
            item.setPrice(BigDecimal.valueOf(100)); // placeholder
            item.setSubtotal(BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItemMapper.insert(item);

            // Remove from cart
            cartService.delete(cartItem.getId(), userId);
        }

        return order;
    }

    public IPage<Order> listByUserId(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedTime);
        return orderMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Order detail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, id));
            order.setItems(items);
        }
        return order;
    }

    @Transactional
    public void cancel(Long orderId, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
                        .eq(Order::getStatus, 0));
        if (order != null) {
            order.setStatus(4); // cancelled
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    @Transactional
    public void confirm(Long orderId, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
                        .eq(Order::getStatus, 2));
        if (order != null) {
            order.setStatus(3); // completed
            order.setReceiveTime(LocalDateTime.now());
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    @Transactional
    public void applyRefund(Long orderId, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId));
        if (order != null && (order.getStatus() == 2 || order.getStatus() == 3)) {
            order.setStatus(5); // refunding
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }
}
