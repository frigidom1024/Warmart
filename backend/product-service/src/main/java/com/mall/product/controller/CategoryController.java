package com.mall.product.controller;

import com.mall.product.common.Result;
import com.mall.product.entity.Category;
import com.mall.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Value("${upload.path:./static/images/categories}")
    private String uploadPath;

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
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            return Result.error(400, "文件名无效");
        }
        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx > 0) {
            ext = originalName.substring(dotIdx).toLowerCase();
        }
        if (!ext.equals(".svg") && !ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg")) {
            return Result.error(400, "仅支持 SVG、PNG、JPG 格式");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(uploadPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path dest = dir.resolve(filename);
            file.transferTo(dest.toFile());
            String url = "/images/categories/" + filename;
            return Result.success(url);
        } catch (IOException e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }
}
