package com.mall.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.product.common.Result;
import com.mall.product.dto.CommentVO;
import com.mall.product.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    @GetMapping("/comment/list/{productId}")
    public Result<IPage<CommentVO>> list(@PathVariable Long productId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.listByProductId(productId, page, size));
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

    @Value("${comment.upload.path:./static/images/comments}")
    private String commentUploadPath;

    @PostMapping("/comment/upload-image")
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
        if (!ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".webp")) {
            return Result.error(400, "仅支持 PNG、JPG、WebP 格式");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(commentUploadPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path dest = dir.resolve(filename);
            file.transferTo(dest.toFile());
            String url = "/images/comments/" + filename;
            return Result.success(url);
        } catch (IOException e) {
            log.error("评价图片上传失败: path={}, filename={}", commentUploadPath, filename, e);
            return Result.error(500, "文件上传失败");
        }
    }
}
