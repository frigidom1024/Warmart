package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.product.entity.Category;
import com.mall.product.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public List<Category> listAll() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort));
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

    public void add(Category category) {
        category.setCreatedTime(LocalDateTime.now());
        if (category.getParentId() == null) category.setParentId(0L);
        if (category.getSort() == null) category.setSort(0);
        if (category.getStatus() == null) category.setStatus(0);
        categoryMapper.insert(category);
    }

    public void update(Category category) {
        categoryMapper.updateById(category);
    }

    public void delete(Long id) {
        // Delete children first
        categoryMapper.delete(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        categoryMapper.deleteById(id);
    }
}
