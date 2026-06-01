package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.product.entity.Category;
import com.mall.product.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public List<Category> listAll() {
        String cacheKey = "product:category:list";

        // Try cache first (gracefully handle Redis down)
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.convertValue(cached, new TypeReference<List<Category>>() {});
            }
        } catch (Exception ignored) {}

        // Query DB
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort));

        // Cache for 1 hour (gracefully handle Redis down)
        try {
            redisTemplate.opsForValue().set(cacheKey, categories, Duration.ofHours(1));
        } catch (Exception ignored) {}

        return categories;
    }

    public void clearCache() {
        try {
            redisTemplate.delete("product:category:list");
        } catch (Exception ignored) {}
    }

    public List<Category> listTree() {
        List<Category> all = listAll();
        List<Category> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());
        for (Category root : roots) {
            root.setChildren(getChildren(root.getId(), all));
        }
        return roots;
    }

    private List<Category> getChildren(Long parentId, List<Category> all) {
        List<Category> children = new ArrayList<>();
        for (Category c : all) {
            if (parentId.equals(c.getParentId())) {
                c.setChildren(getChildren(c.getId(), all));
                children.add(c);
            }
        }
        return children;
    }

    public List<Long> getRecursiveCategoryIds(Long parentId) {
        List<Category> all = listAll();
        List<Long> ids = new ArrayList<>();
        ids.add(parentId);
        collectChildIds(parentId, all, ids);
        return ids;
    }

    private void collectChildIds(Long parentId, List<Category> all, List<Long> ids) {
        for (Category c : all) {
            if (parentId.equals(c.getParentId())) {
                ids.add(c.getId());
                collectChildIds(c.getId(), all, ids);
            }
        }
    }

    public void add(Category category) {
        LocalDateTime now = LocalDateTime.now();
        category.setCreatedTime(now);
        category.setUpdatedTime(now);
        if (category.getParentId() == null) category.setParentId(0L);
        if (category.getSort() == null) category.setSort(0);
        if (category.getStatus() == null) category.setStatus(0);
        categoryMapper.insert(category);
        clearCache();
    }

    public void update(Category category) {
        categoryMapper.updateById(category);
        clearCache();
    }

    public void delete(Long id) {
        // Delete children first
        categoryMapper.delete(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        categoryMapper.deleteById(id);
        clearCache();
    }
}
