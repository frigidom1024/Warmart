package com.mall.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.product.common.Result;
import com.mall.product.entity.*;
import com.mall.product.mapper.ProductMapper;
import com.mall.product.mapper.ProductSpecMapper;
import com.mall.product.service.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductSpecMapper productSpecMapper;
    private final CommentService commentService;
    private final FavoriteService favoriteService;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;

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
        try {
            String url = fileStorageService.uploadFile(file, "products");
            return Result.success(url);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    // ─── Excel export / import ───

    @GetMapping("/admin/export")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) throws IOException {

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getName, keyword);
        }
        wrapper.orderByDesc(Product::getCreatedTime);

        List<Product> products = productMapper.selectList(wrapper);

        // Resolve category names
        List<Category> allCategories = categoryService.listAll();
        Map<Long, String> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        List<ProductExportVO> exportList = products.stream().map(p -> {
            ProductExportVO vo = new ProductExportVO();
            vo.setName(p.getName());
            vo.setCategoryName(categoryMap.getOrDefault(p.getCategoryId(), ""));
            vo.setPrice(p.getPrice() != null ? p.getPrice().doubleValue() : null);
            vo.setOriginalPrice(p.getOriginalPrice() != null ? p.getOriginalPrice().doubleValue() : null);
            vo.setStock(p.getStock());
            vo.setSales(p.getSales());
            vo.setDescription(p.getDescription());
            vo.setMainImage(p.getMainImage());
            vo.setTag(p.getTag());
            vo.setStatusText(p.getStatus() != null && p.getStatus() == 0 ? "上架" : "下架");
            vo.setRecommendText(p.getIsRecommend() != null && p.getIsRecommend() == 1 ? "是" : "否");
            return vo;
        }).collect(Collectors.toList());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("商品数据", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), ProductExportVO.class)
                .sheet("商品数据")
                .doWrite(exportList);
    }

    @PostMapping("/admin/import")
    public Result<Map<String, Object>> importProducts(@RequestParam("file") MultipartFile file) throws IOException {
        // Validate file format
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".xlsx")) {
            return Result.error(400, "仅支持 .xlsx 格式文件");
        }

        // Read Excel rows
        List<ProductImportRow> rows = EasyExcel.read(file.getInputStream(), ProductImportRow.class, null)
                .sheet()
                .doReadSync();

        if (rows == null || rows.isEmpty()) {
            return Result.error(400, "文件无数据");
        }

        // Build category name-to-id map
        List<Category> allCategories = categoryService.listAll();
        Map<String, Long> categoryNameToId = allCategories.stream()
                .collect(Collectors.toMap(Category::getName, Category::getId, (a, b) -> a));

        int total = rows.size();
        int success = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            ProductImportRow row = rows.get(i);
            int rowNum = i + 2; // Excel row number (1-indexed + header row)

            // Validate required fields
            if (row.getName() == null || row.getName().isEmpty()) {
                errors.add("第" + rowNum + "行：商品名称为空");
                continue;
            }
            if (row.getPrice() == null) {
                errors.add("第" + rowNum + "行：商品价格为空");
                continue;
            }
            if (row.getStock() == null) {
                errors.add("第" + rowNum + "行：商品库存为空");
                continue;
            }

            // Resolve category name to ID
            Long categoryId = null;
            if (row.getCategoryName() != null && !row.getCategoryName().isEmpty()) {
                categoryId = categoryNameToId.get(row.getCategoryName());
                if (categoryId == null) {
                    errors.add("第" + rowNum + "行：分类 '" + row.getCategoryName() + "' 不存在");
                    continue;
                }
            }

            // Match existing product by name
            Product existing = productMapper.selectOne(
                    new LambdaQueryWrapper<Product>().eq(Product::getName, row.getName()));

            if (existing != null) {
                // Update existing product
                existing.setCategoryId(categoryId);
                if (row.getPrice() != null) existing.setPrice(BigDecimal.valueOf(row.getPrice()));
                if (row.getOriginalPrice() != null) existing.setOriginalPrice(BigDecimal.valueOf(row.getOriginalPrice()));
                if (row.getStock() != null) existing.setStock(row.getStock());
                if (row.getSales() != null) existing.setSales(row.getSales());
                if (row.getDescription() != null) existing.setDescription(row.getDescription());
                if (row.getMainImage() != null) existing.setMainImage(row.getMainImage());
                if (row.getTag() != null) existing.setTag(row.getTag());
                if (row.getStatus() != null) existing.setStatus(row.getStatus());
                existing.setUpdatedTime(LocalDateTime.now());
                productMapper.updateById(existing);
            } else {
                // Create new product
                Product product = new Product();
                product.setCategoryId(categoryId);
                product.setName(row.getName());
                product.setPrice(BigDecimal.valueOf(row.getPrice()));
                if (row.getOriginalPrice() != null) product.setOriginalPrice(BigDecimal.valueOf(row.getOriginalPrice()));
                product.setStock(row.getStock());
                product.setSales(row.getSales() != null ? row.getSales() : 0);
                product.setDescription(row.getDescription());
                product.setMainImage(row.getMainImage());
                product.setTag(row.getTag());
                product.setStatus(row.getStatus() != null ? row.getStatus() : 0);
                product.setCreatedTime(LocalDateTime.now());
                product.setUpdatedTime(LocalDateTime.now());
                productMapper.insert(product);
            }
            success++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", success);
        result.put("totalCount", total);
        result.put("errorList", errors);

        return Result.success(result);
    }

    // ─── Inner class for import rows ───

    @Data
    public static class ProductImportRow {
        @ExcelProperty("商品名称")
        private String name;

        @ExcelProperty("分类")
        private String categoryName;

        @ExcelProperty("价格")
        private Double price;

        @ExcelProperty("原价")
        private Double originalPrice;

        @ExcelProperty("库存")
        private Integer stock;

        @ExcelProperty("销量")
        private Integer sales;

        @ExcelProperty("商品描述")
        private String description;

        @ExcelProperty("主图URL")
        private String mainImage;

        @ExcelProperty("标签")
        private String tag;

        @ExcelProperty("状态(0=上架,1=下架)")
        private Integer status;
    }
}
