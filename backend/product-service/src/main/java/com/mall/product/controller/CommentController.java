package com.mall.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.common.Result;
import com.mall.product.dto.CommentVO;
import com.mall.product.service.CommentService;
import com.mall.product.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final FileStorageService fileStorageService;

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    @GetMapping("/comment/list/{productId}")
    public Result<IPage<CommentVO>> list(@PathVariable Long productId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.listByProductId(productId, page, size));
    }

    @GetMapping("/admin/comment/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<CommentVO>> adminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer rating) {
        return Result.success(commentService.adminList(page, size, productName, username, rating));
    }

    @DeleteMapping("/admin/comment/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteById(id);
        return Result.success(null);
    }

    @PostMapping("/comment/add")
    public Result<Void> add(@AuthenticationPrincipal Jwt jwt,
                             @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(jwt.getSubject());
        Long productId = Long.valueOf(body.get("productId").toString());
        String content = (String) body.get("content");
        Integer rating = Integer.valueOf(body.get("rating").toString());
        String imageUrls = (String) body.getOrDefault("imageUrls", "");
        commentService.add(productId, userId, content, rating, imageUrls);
        return Result.success(null);
    }

    @PostMapping("/comment/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = fileStorageService.uploadFile(file, "comments");
            return Result.success(url);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("评价图片上传失败", e);
            return Result.error(500, "文件上传失败");
        }
    }
}
