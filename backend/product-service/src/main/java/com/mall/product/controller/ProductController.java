package com.mall.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.common.Result;
import com.mall.product.entity.Product;
import com.mall.product.entity.ProductSpec;
import com.mall.product.service.ProductService;
import com.mall.product.service.CommentService;
import com.mall.product.service.FavoriteService;
import com.mall.product.entity.Comment;
import com.mall.product.mapper.ProductSpecMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductSpecMapper productSpecMapper;
    private final CommentService commentService;
    private final FavoriteService favoriteService;

    @GetMapping("/list")
    public Result<IPage<Product>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "new") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(productService.list(categoryId, sortBy, page, size));
    }

    @GetMapping("/detail/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }

    @PostMapping("/search")
    public Result<IPage<Product>> search(@RequestBody Map<String, Object> params) {
        String keyword = (String) params.get("keyword");
        Long categoryId = params.get("categoryId") != null ? Long.valueOf(params.get("categoryId").toString()) : null;
        BigDecimal minPrice = params.get("minPrice") != null ? new BigDecimal(params.get("minPrice").toString()) : null;
        BigDecimal maxPrice = params.get("maxPrice") != null ? new BigDecimal(params.get("maxPrice").toString()) : null;
        String sortBy = (String) params.getOrDefault("sortBy", "new");
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 20;
        return Result.success(productService.search(keyword, categoryId, minPrice, maxPrice, sortBy, page, size));
    }

    @GetMapping("/spec/list/{productId}")
    public Result<List<ProductSpec>> specs(@PathVariable Long productId) {
        return Result.success(productSpecMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductSpec>()
                        .eq(ProductSpec::getProductId, productId)
                        .orderByAsc(ProductSpec::getSort)));
    }
}
