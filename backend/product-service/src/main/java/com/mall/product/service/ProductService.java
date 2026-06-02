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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ProductMapper productMapper;
    private final ProductSpecMapper productSpecMapper;
    private final SpecGroupMapper specGroupMapper;
    private final SpecValueMapper specValueMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductImageMapper productImageMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CategoryService categoryService;

    public IPage<Product> list(Long categoryId, String sortBy, int page, int size) {
        Page<Product> p = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);

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
            return OBJECT_MAPPER.convertValue(cached, Product.class);
        }

        // Query DB
        Product product = productMapper.selectById(id);
        if (product != null) {
            // Load spec groups
            List<SpecGroup> specGroups = specGroupMapper.selectList(
                    new LambdaQueryWrapper<SpecGroup>()
                            .eq(SpecGroup::getProductId, id)
                            .orderByAsc(SpecGroup::getSort));

            // Batch load spec values (N+1 fix)
            if (!specGroups.isEmpty()) {
                List<Long> groupIds = specGroups.stream().map(SpecGroup::getId).collect(Collectors.toList());
                List<SpecValue> allValues = specValueMapper.selectList(
                        new LambdaQueryWrapper<SpecValue>()
                                .in(SpecValue::getGroupId, groupIds)
                                .orderByAsc(SpecValue::getSort));
                Map<Long, List<SpecValue>> valueMap = allValues.stream()
                        .collect(Collectors.groupingBy(SpecValue::getGroupId));
                for (SpecGroup g : specGroups) {
                    g.setValues(valueMap.getOrDefault(g.getId(), List.of()));
                }
            }
            product.setSpecGroups(specGroups);

            // Load SKUs
            List<ProductSku> skus = productSkuMapper.selectList(
                    new LambdaQueryWrapper<ProductSku>()
                            .eq(ProductSku::getProductId, id)
                            .orderByAsc(ProductSku::getSort));
            skus.forEach(ProductSku::parseSpecValueIds);
            product.setSkuList(skus);

            // Load images
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
                                  BigDecimal maxPrice, Boolean exactMatch, String sortBy, int page, int size) {
        Page<Product> p = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1);

        if (keyword != null && !keyword.isEmpty()) {
            if (Boolean.TRUE.equals(exactMatch)) {
                wrapper.eq(Product::getName, keyword);
            } else {
                wrapper.and(w -> w.like(Product::getName, keyword)
                        .or().like(Product::getDescription, keyword));
            }
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

        // Insert spec groups and values
        if (product.getSpecGroups() != null) {
            for (SpecGroup g : product.getSpecGroups()) {
                g.setProductId(product.getId());
                specGroupMapper.insert(g);
                if (g.getValues() != null) {
                    for (SpecValue v : g.getValues()) {
                        v.setGroupId(g.getId());
                        specValueMapper.insert(v);
                    }
                }
            }
        }
        // Insert SKUs
        if (product.getSkuList() != null) {
            for (ProductSku s : product.getSkuList()) {
                s.setProductId(product.getId());
                productSkuMapper.insert(s);
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
        // Evict cache so next detail fetch gets fresh data
        try { redisTemplate.delete("product:detail:" + product.getId()); } catch (Exception ignored) {}

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
        productSkuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
        // Delete spec values then groups
        List<SpecGroup> groups = specGroupMapper.selectList(
                new LambdaQueryWrapper<SpecGroup>().eq(SpecGroup::getProductId, id));
        for (SpecGroup g : groups) {
            specValueMapper.delete(new LambdaQueryWrapper<SpecValue>().eq(SpecValue::getGroupId, g.getId()));
        }
        specGroupMapper.delete(new LambdaQueryWrapper<SpecGroup>().eq(SpecGroup::getProductId, id));
        productSpecMapper.delete(new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, id));
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id));
        productMapper.deleteById(id);
    }

    public List<Product> listRecommended() {
        return productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .eq(Product::getIsRecommend, 1)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT 10"));
    }

    public List<Product> listByCategoryId(Long categoryId) {
        List<Long> ids = categoryService.getRecursiveCategoryIds(categoryId);
        return productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .in(Product::getCategoryId, ids)
                        .eq(Product::getStatus, 1)
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
