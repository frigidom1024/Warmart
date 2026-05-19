package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.product.dto.FavoriteVO;
import com.mall.product.entity.Favorite;
import com.mall.product.entity.Product;
import com.mall.product.mapper.FavoriteMapper;
import com.mall.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;

    public void add(Long userId, Long productId) {
        // Check if already favorited
        Long count = favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, productId));
        if (count == 0) {
            Favorite fav = new Favorite();
            fav.setUserId(userId);
            fav.setProductId(productId);
            fav.setCreatedTime(LocalDateTime.now());
            favoriteMapper.insert(fav);
        }
    }

    public void cancel(Long userId, Long productId) {
        favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, productId));
    }

    public IPage<FavoriteVO> listByUserId(Long userId, int page, int size) {
        Page<Favorite> favPage = favoriteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedTime));

        Page<FavoriteVO> voPage = new Page<>(favPage.getCurrent(), favPage.getSize(), favPage.getTotal());
        if (favPage.getRecords().isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        List<Long> productIds = favPage.getRecords().stream()
                .map(Favorite::getProductId).collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getId, productIds))
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<FavoriteVO> vos = favPage.getRecords().stream().map(fav -> {
            FavoriteVO vo = new FavoriteVO();
            vo.setId(fav.getId());
            vo.setUserId(fav.getUserId());
            vo.setProductId(fav.getProductId());
            vo.setCreatedTime(fav.getCreatedTime());

            Product p = productMap.get(fav.getProductId());
            if (p != null) {
                vo.setProductName(p.getName());
                vo.setProductPrice(p.getPrice());
                vo.setProductOldPrice(p.getOriginalPrice());
                vo.setProductImage(p.getMainImage());
                vo.setProductTag(p.getTag());
                vo.setProductSales(p.getSales());
            }
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(vos);
        return voPage;
    }

    public boolean isFavorited(Long userId, Long productId) {
        return favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getProductId, productId)) > 0;
    }
}
