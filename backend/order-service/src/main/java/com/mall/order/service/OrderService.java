package com.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.order.entity.Cart;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.mapper.CartMapper;
import com.mall.order.mapper.OrderItemMapper;
import com.mall.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartService cartService;
    private final CartMapper cartMapper;
    private final RestTemplate restTemplate;

    @Transactional
    public Order create(Long userId, String receiverName, String receiverPhone,
                        String receiverAddress, String paymentMethod, List<Long> cartItemIds) {
        // Get cart items by IDs (instead of getCheckedItems)
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new RuntimeException("No items selected");
        }
        List<Cart> cartItems = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .in(Cart::getId, cartItemIds)
                        .eq(Cart::getUserId, userId));
        if (cartItems.isEmpty()) {
            throw new RuntimeException("No items selected");
        }

        // Generate order no
        String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%08d", new Random().nextInt(100000000));

        // Calculate total and fetch product details
        List<Long> productIds = cartItems.stream()
                .map(Cart::getProductId).collect(Collectors.toList());

        Map<Long, Map<String, Object>> productMap = fetchProductMap(productIds);

        BigDecimal total = BigDecimal.ZERO;
        for (var cartItem : cartItems) {
            Map<String, Object> p = productMap.get(cartItem.getProductId());
            BigDecimal price = p != null ? new BigDecimal(p.get("price").toString()) : BigDecimal.valueOf(100);
            total = total.add(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        // Create order
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setTotalAmount(total);
        order.setStatus(0);
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

            Map<String, Object> p = productMap.get(cartItem.getProductId());
            if (p != null) {
                item.setProductName((String) p.getOrDefault("name", "商品"));
                item.setProductImage((String) p.getOrDefault("mainImage", null));
                BigDecimal price = new BigDecimal(p.get("price").toString());
                item.setPrice(price);
                item.setSubtotal(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            } else {
                item.setProductName("Product " + cartItem.getProductId());
                item.setPrice(BigDecimal.valueOf(100));
                item.setSubtotal(BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }
            item.setQuantity(cartItem.getQuantity());
            item.setSpecInfo(cartItem.getSpecInfo());  // Pass specInfo through
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
        IPage<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);

        // Populate items for each order
        for (Order order : orderPage.getRecords()) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, order.getId()));
            order.setItems(items);
        }
        return orderPage;
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

    public IPage<Order> adminList(Integer status, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedTime);
        IPage<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), wrapper);

        for (Order order : orderPage.getRecords()) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, order.getId()));
            order.setItems(items);
        }
        return orderPage;
    }

    @Transactional
    public void adminUpdateStatus(Long id, Integer status) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            order.setStatus(status);
            order.setUpdatedTime(LocalDateTime.now());
            if (status == 3) order.setReceiveTime(LocalDateTime.now());
            if (status >= 1 && order.getPaymentTime() == null) order.setPaymentTime(LocalDateTime.now());
            if (status >= 2 && order.getDeliveryTime() == null) order.setDeliveryTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Map<String, Object>> fetchProductMap(List<Long> productIds) {
        try {
            String url = "http://product-service:8083/api/product/inner/listByIds";
            var response = restTemplate.postForEntity(url, productIds, Map.class);
            Map<String, Object> result = response.getBody();
            if (result != null && Integer.valueOf(200).equals(result.get("code"))) {
                List<Map<String, Object>> products = (List<Map<String, Object>>) result.get("data");
                if (products != null) {
                    return products.stream()
                            .collect(Collectors.toMap(
                                    p -> Long.valueOf(p.get("id").toString()),
                                    p -> p));
                }
            }
        } catch (Exception ignored) {}
        return Map.of();
    }
}
