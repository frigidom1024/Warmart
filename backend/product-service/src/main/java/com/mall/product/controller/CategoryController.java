package com.mall.product.controller;

import com.mall.product.common.Result;
import com.mall.product.entity.Category;
import com.mall.product.service.CategoryService;
import com.mall.product.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final FileStorageService fileStorageService;

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

    @PostMapping("/category/admin/upload-icon")
    public Result<String> uploadIcon(@RequestParam("file") MultipartFile file) {
        try {
            String url = fileStorageService.uploadFile(file, "categories");
            return Result.success(url);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }
}
