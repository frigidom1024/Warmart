package com.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mall.order.dto.CartVO;
import com.mall.order.entity.Cart;
import com.mall.order.mapper.CartMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;
    private final RestTemplate restTemplate;

    public List<CartVO> listByUserId(Long userId) {
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getCreatedTime));
        if (carts.isEmpty()) return Collections.emptyList();

        List<Long> productIds = carts.stream()
                .map(Cart::getProductId).collect(Collectors.toList());

        // Fetch product info from product-service
        Map<Long, Map<String, Object>> productMap = fetchProductMap(productIds);

        return carts.stream().map(cart -> {
            CartVO vo = new CartVO();
            vo.setId(cart.getId());
            vo.setUserId(cart.getUserId());
            vo.setProductId(cart.getProductId());
            vo.setQuantity(cart.getQuantity());
            vo.setChecked(cart.getChecked());
            vo.setCreatedTime(cart.getCreatedTime());
            vo.setUpdatedTime(cart.getUpdatedTime());

            Map<String, Object> p = productMap.get(cart.getProductId());
            if (p != null) {
                vo.setProductName((String) p.get("name"));
                vo.setProductPrice(new BigDecimal(p.get("price").toString()));
                vo.setProductImage((String) p.get("mainImage"));
                vo.setProductTag((String) p.get("tag"));
                if (p.get("originalPrice") != null) {
                    vo.setProductOldPrice(new BigDecimal(p.get("originalPrice").toString()));
                }
            }
            return vo;
        }).collect(Collectors.toList());
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
        return Collections.emptyMap();
    }

    @Transactional
    public void add(Long userId, Long productId, Integer quantity) {
        // Check if product already in cart
        Cart existing = cartMapper.selectOne(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .eq(Cart::getProductId, productId));
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            existing.setUpdatedTime(LocalDateTime.now());
            cartMapper.updateById(existing);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            cart.setChecked(1);
            cart.setCreatedTime(LocalDateTime.now());
            cart.setUpdatedTime(LocalDateTime.now());
            cartMapper.insert(cart);
        }
    }

    @Transactional
    public void updateQuantity(Long id, Long userId, Integer quantity) {
        cartMapper.update(null,
                new LambdaUpdateWrapper<Cart>()
                        .eq(Cart::getId, id)
                        .eq(Cart::getUserId, userId)
                        .set(Cart::getQuantity, quantity)
                        .set(Cart::getUpdatedTime, LocalDateTime.now()));
    }

    @Transactional
    public void delete(Long id, Long userId) {
        cartMapper.delete(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getId, id)
                        .eq(Cart::getUserId, userId));
    }

    @Transactional
    public void check(Long id, Long userId, Integer checked) {
        cartMapper.update(null,
                new LambdaUpdateWrapper<Cart>()
                        .eq(Cart::getId, id)
                        .eq(Cart::getUserId, userId)
                        .set(Cart::getChecked, checked));
    }

    @Transactional
    public void checkAll(Long userId, Integer checked) {
        cartMapper.update(null,
                new LambdaUpdateWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .set(Cart::getChecked, checked));
    }

    public List<Cart> getCheckedItems(Long userId) {
        return cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .eq(Cart::getChecked, 1));
    }
}
