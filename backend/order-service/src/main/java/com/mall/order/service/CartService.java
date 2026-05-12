package com.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mall.order.entity.Cart;
import com.mall.order.mapper.CartMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartMapper cartMapper;

    public List<Cart> listByUserId(Long userId) {
        return cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getCreatedTime));
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
