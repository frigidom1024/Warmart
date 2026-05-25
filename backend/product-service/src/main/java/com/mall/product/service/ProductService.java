package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mall.product.entity.*;
import com.mall.product.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductSpecMapper productSpecMapper;
    private final ProductImageMapper productImageMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CategoryService categoryService;

    public IPage<Product> list(Long categoryId, String sortBy, int page, int size) {
        Page<Product> p = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 0);

        if (categoryId != null) {
            List<Long> ids = categoryService.getRecursiveCategoryIds(categoryId);
            wrapper.in(Product::getCategoryId, ids);
        }

        switch (sortBy != null ? sortBy : "new") {
            case "price_asc":
                wrapper.orderByAsc(Product::getPrice);
                break;
            case "price_desc":
                wrapper.orderByDesc(Product::getPrice);
                break;
            case "sales_desc":
                wrapper.orderByDesc(Product::getSales);
                break;
            default:
                wrapper.orderByDesc(Product::getCreatedTime);
                break;
        }

        return productMapper.selectPage(p, wrapper);
    }

    public Product detail(Long id) {
        String cacheKey = "product:detail:" + id;

        // Try cache first
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.convertValue(cached, Product.class);
        }

        // Query DB
        Product product = productMapper.selectById(id);
        if (product != null) {
            List<ProductSpec> specs = productSpecMapper.selectList(
                    new LambdaQueryWrapper<ProductSpec>()
                            .eq(ProductSpec::getProductId, id)
                            .orderByAsc(ProductSpec::getSort));
            product.setSpecList(specs);

            List<ProductImage> images = productImageMapper.selectList(
                    new LambdaQueryWrapper<ProductImage>()
                            .eq(ProductImage::getProductId, id)
                            .orderByAsc(ProductImage::getSort));
            product.setImageList(images);

            // Cache for 30 minutes
            redisTemplate.opsForValue().set(cacheKey, product, Duration.ofMinutes(30));
        }
        return product;
    }

    public IPage<Product> search(String keyword, Long categoryId, BigDecimal minPrice,
                                  BigDecimal maxPrice, String sortBy, int page, int size) {
        Page<Product> p = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 0);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Product::getName, keyword)
                    .or().like(Product::getDescription, keyword));
        }
        if (categoryId != null) {
            List<Long> ids = categoryService.getRecursiveCategoryIds(categoryId);
            wrapper.in(Product::getCategoryId, ids);
        }
        if (minPrice != null) {
            wrapper.ge(Product::getPrice, minPrice);
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, maxPrice);
        }

        switch (sortBy != null ? sortBy : "new") {
            case "price_asc": wrapper.orderByAsc(Product::getPrice); break;
            case "price_desc": wrapper.orderByDesc(Product::getPrice); break;
            case "sales_desc": wrapper.orderByDesc(Product::getSales); break;
            default: wrapper.orderByDesc(Product::getCreatedTime); break;
        }

        return productMapper.selectPage(p, wrapper);
    }

    @Transactional
    public void add(Product product) {
        product.setSales(0);
        product.setCreatedTime(LocalDateTime.now());
        product.setUpdatedTime(LocalDateTime.now());
        productMapper.insert(product);

        // Insert specs
        if (product.getSpecList() != null) {
            for (ProductSpec spec : product.getSpecList()) {
                spec.setProductId(product.getId());
                productSpecMapper.insert(spec);
            }
        }
        // Insert images
        if (product.getImageList() != null) {
            for (ProductImage image : product.getImageList()) {
                image.setProductId(product.getId());
                productImageMapper.insert(image);
            }
        }
    }

    @Transactional
    public void update(Product product) {
        product.setUpdatedTime(LocalDateTime.now());
        productMapper.updateById(product);

        // Cascade update specs: delete old, insert new
        if (product.getSpecList() != null) {
            productSpecMapper.delete(new LambdaQueryWrapper<ProductSpec>()
                    .eq(ProductSpec::getProductId, product.getId()));
            for (ProductSpec spec : product.getSpecList()) {
                spec.setId(null);
                spec.setProductId(product.getId());
                productSpecMapper.insert(spec);
            }
        }
        // Cascade update images: delete old, insert new
        if (product.getImageList() != null) {
            productImageMapper.delete(new LambdaQueryWrapper<ProductImage>()
                    .eq(ProductImage::getProductId, product.getId()));
            for (ProductImage image : product.getImageList()) {
                image.setId(null);
                image.setProductId(product.getId());
                productImageMapper.insert(image);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        productSpecMapper.delete(new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, id));
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id));
        productMapper.deleteById(id);
    }

    public List<Product> listRecommended() {
        return productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 0)
                        .eq(Product::getIsRecommend, 1)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT 10"));
    }

    public List<Product> listByCategoryId(Long categoryId) {
        List<Long> ids = categoryService.getRecursiveCategoryIds(categoryId);
        return productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .in(Product::getCategoryId, ids)
                        .eq(Product::getStatus, 0)
                        .orderByDesc(Product::getCreatedTime));
    }

    public List<Product> listByIds(List<Long> ids) {
        return productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .in(Product::getId, ids));
    }

    public IPage<Product> adminList(Long categoryId, Integer status, int page, int size) {
        Page<Product> p = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreatedTime);
        return productMapper.selectPage(p, wrapper);
    }
}
