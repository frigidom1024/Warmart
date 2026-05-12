package com.mall.product.controller;

import com.mall.product.common.Result;
import com.mall.product.entity.Category;
import com.mall.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
