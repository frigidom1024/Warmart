package com.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.order.entity.Cart;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderExportVO;
import com.mall.order.entity.OrderItem;
import com.mall.order.entity.RefundApplication;
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
    private final RefundService refundService;
    private final LogisticsService logisticsService;

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

    /**
     * Transition order to a new status: updates status, sets time fields,
     * saves the order, and auto-creates corresponding logistics tracks.
     * Caller is responsible for loading the order and any pre-update mutations.
     */
    private void transitionStatus(Order order, int newStatus) {
        int oldStatus = order.getStatus();
        if (oldStatus == newStatus) return;

        order.setStatus(newStatus);
        order.setUpdatedTime(LocalDateTime.now());
        if (newStatus >= 1 && order.getPaymentTime() == null) order.setPaymentTime(LocalDateTime.now());
        if (newStatus >= 2 && order.getDeliveryTime() == null) order.setDeliveryTime(LocalDateTime.now());
        if (newStatus == 3) order.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // Auto-create logistics tracks for key milestones
        if (newStatus == 3 && oldStatus == 2) {
            logisticsService.addTrack(order.getId(), "DELIVERED", "订单已签收，确认收货",
                    order.getReceiverAddress(), LocalDateTime.now());
        }
    }

    @Transactional
    public void cancel(Long orderId, Long userId) {
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getId, orderId)
                        .eq(Order::getUserId, userId)
                        .eq(Order::getStatus, 0));
        if (order != null) {
            transitionStatus(order, 4);
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
            transitionStatus(order, 3);
        }
    }

    @Transactional
    public void applyRefund(Long orderId, Long userId, String reason) {
        refundService.create(orderId, userId, reason);
    }

    public void cancelRefund(Long orderId, Long userId) {
        refundService.cancelByOrderId(orderId, userId);
    }

    public RefundApplication getRefundInfo(Long orderId) {
        return refundService.getByOrderId(orderId);
    }

    public IPage<Order> adminList(Integer status, String orderNo, String receiverName,
                                   String receiverPhone, String startTime, String endTime,
                                   int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Order::getStatus, status);
        if (orderNo != null && !orderNo.isEmpty()) wrapper.like(Order::getOrderNo, orderNo);
        if (receiverName != null && !receiverName.isEmpty()) wrapper.like(Order::getReceiverName, receiverName);
        if (receiverPhone != null && !receiverPhone.isEmpty()) wrapper.like(Order::getReceiverPhone, receiverPhone);
        if (startTime != null && !startTime.isEmpty()) wrapper.ge(Order::getCreatedTime, LocalDateTime.parse(startTime));
        if (endTime != null && !endTime.isEmpty()) wrapper.le(Order::getCreatedTime, LocalDateTime.parse(endTime));
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

    @Transactional
    public void adminShip(Long id, String logisticsCompany, String logisticsNo) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }
        order.setLogisticsCompany(logisticsCompany);
        order.setLogisticsNo(logisticsNo);

        // Generate initial logistics tracks before status transition
        logisticsService.addTrack(id, "ORDERED", "订单已确认", null, order.getCreatedTime());
        logisticsService.addTrack(id, "WAREHOUSE", "商品已打包完成，等待快递揽收", null, LocalDateTime.now());

        transitionStatus(order, 2);
    }

    @Transactional
    public void adminCancel(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }
        int s = order.getStatus();
        // Admin can cancel: pending payment(0), pending delivery(1), or refunding(5)
        if (s != 0 && s != 1 && s != 5) {
            throw new RuntimeException("Current order status does not allow cancellation");
        }
        transitionStatus(order, 4);
    }

    public List<OrderExportVO> exportList(Integer status, String orderNo, String receiverName,
                                           String receiverPhone, String startTime, String endTime) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Order::getStatus, status);
        if (orderNo != null && !orderNo.isEmpty()) wrapper.like(Order::getOrderNo, orderNo);
        if (receiverName != null && !receiverName.isEmpty()) wrapper.like(Order::getReceiverName, receiverName);
        if (receiverPhone != null && !receiverPhone.isEmpty()) wrapper.like(Order::getReceiverPhone, receiverPhone);
        if (startTime != null && !startTime.isEmpty()) wrapper.ge(Order::getCreatedTime, LocalDateTime.parse(startTime));
        if (endTime != null && !endTime.isEmpty()) wrapper.le(Order::getCreatedTime, LocalDateTime.parse(endTime));
        wrapper.orderByDesc(Order::getCreatedTime);
        List<Order> orders = orderMapper.selectList(wrapper);

        String[] statusTexts = {"待付款", "待发货", "待收货", "已完成", "已取消", "退款中"};
        List<OrderExportVO> result = new java.util.ArrayList<>();
        for (Order order : orders) {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

            OrderExportVO vo = new OrderExportVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setReceiverName(order.getReceiverName());
            vo.setReceiverPhone(order.getReceiverPhone());
            vo.setReceiverAddress(order.getReceiverAddress());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setStatusText(order.getStatus() >= 0 && order.getStatus() < statusTexts.length ? statusTexts[order.getStatus()] : "未知");
            vo.setPaymentMethod(order.getPaymentMethod());
            vo.setPaymentTime(order.getPaymentTime() != null ? order.getPaymentTime().toString().replace("T", " ") : "");
            vo.setDeliveryTime(order.getDeliveryTime() != null ? order.getDeliveryTime().toString().replace("T", " ") : "");
            vo.setReceiveTime(order.getReceiveTime() != null ? order.getReceiveTime().toString().replace("T", " ") : "");
            vo.setLogisticsCompany(order.getLogisticsCompany());
            vo.setLogisticsNo(order.getLogisticsNo());
            vo.setCreatedTime(order.getCreatedTime() != null ? order.getCreatedTime().toString().replace("T", " ") : "");

            // Build product info string: name x qty; name x qty
            StringBuilder sb = new StringBuilder();
            for (OrderItem item : items) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(item.getProductName());
                if (item.getSpecInfo() != null) sb.append("(").append(item.getSpecInfo()).append(")");
                sb.append(" x").append(item.getQuantity());
            }
            vo.setProductInfo(sb.toString());
            result.add(vo);
        }
        return result;
    }

    // ─── Dashboard stats ───

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new java.util.HashMap<>();

        // Total orders
        stats.put("totalOrders", orderMapper.selectCount(null));

        // Total sales (completed orders)
        stats.put("totalSales", orderMapper.sumTotalSales());

        // Today stats
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        stats.put("todayOrders", orderMapper.countOrdersBetween(todayStart, todayEnd));
        stats.put("todaySales", orderMapper.sumSalesBetween(todayStart, todayEnd));

        // Order status distribution
        stats.put("orderStatusStats", orderMapper.countByStatus());

        // Sales trend (last 14 days)
        LocalDateTime twoWeeksAgo = todayStart.minusDays(13);
        stats.put("salesTrend", orderMapper.dailySalesSince(twoWeeksAgo));

        // Hot products (top 10)
        stats.put("hotProducts", orderItemMapper.hotProducts(10));

        return stats;
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
