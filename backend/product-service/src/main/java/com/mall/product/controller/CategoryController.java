package com.mall.product.controller;

import com.mall.product.common.Result;
import com.mall.product.entity.Category;
import com.mall.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/category/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listTree());
    }

    // ─── Admin endpoints ───

    @GetMapping("/category/admin/list")
    public Result<List<Category>> adminList() {
        return Result.success(categoryService.listAll());
    }

    @PostMapping("/category/admin/add")
    public Result<Void> adminAdd(@RequestBody Category category) {
        categoryService.add(category);
        return Result.success(null);
    }

    @PutMapping("/category/admin/update")
    public Result<Void> adminUpdate(@RequestBody Category category) {
        categoryService.update(category);
        return Result.success(null);
    }

    @DeleteMapping("/category/admin/delete/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success(null);
    }
}
