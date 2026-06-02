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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductSpecMapper productSpecMapper;
    private final CommentService commentService;
    private final FavoriteService favoriteService;

    @Value("${upload.path:./static/images/products}")
    private String uploadPath;

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
        boolean exactMatch = params.get("exactMatch") != null && Boolean.parseBoolean(params.get("exactMatch").toString());
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 20;
        return Result.success(productService.search(keyword, categoryId, minPrice, maxPrice, exactMatch, sortBy, page, size));
    }

    @GetMapping("/spec/list/{productId}")
    public Result<List<ProductSpec>> specs(@PathVariable Long productId) {
        return Result.success(productSpecMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProductSpec>()
                        .eq(ProductSpec::getProductId, productId)
                        .orderByAsc(ProductSpec::getSort)));
    }

    @PostMapping("/inner/listByIds")
    public Result<List<Product>> listByIds(@RequestBody List<Long> ids) {
        return Result.success(productService.listByIds(ids));
    }

    // ─── Admin endpoints ───

    @GetMapping("/admin/list")
    public Result<IPage<Product>> adminList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(productService.adminList(categoryId, status, page, size));
    }

    @PostMapping("/admin/add")
    public Result<Void> adminAdd(@RequestBody Product product) {
        productService.add(product);
        return Result.success(null);
    }

    @PutMapping("/admin/update")
    public Result<Void> adminUpdate(@RequestBody Product product) {
        productService.update(product);
        return Result.success(null);
    }

    @DeleteMapping("/admin/delete/{id}")
    public Result<Void> adminDelete(@PathVariable Long id) {
        productService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/admin/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
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
        if (!ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".webp") && !ext.equals(".svg")) {
            return Result.error(400, "仅支持 PNG、JPG、WebP、SVG 格式");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(uploadPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path dest = dir.resolve(filename);
            file.transferTo(dest.toFile());
            String url = "/images/products/" + filename;
            return Result.success(url);
        } catch (IOException e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }
}
