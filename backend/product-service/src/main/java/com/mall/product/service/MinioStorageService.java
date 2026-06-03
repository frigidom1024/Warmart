package com.mall.product.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.public-url}")
    private String publicUrl;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".webp", ".svg"
    );

    @Override
    public String uploadFile(MultipartFile file, String subdirectory) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            throw new IllegalArgumentException("文件名无效");
        }

        String ext = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件格式: " + ext
                    + "，仅支持 PNG、JPG、WebP、SVG");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        String objectName = subdirectory + "/" + filename;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(detectContentType(ext))
                            .build()
            );
            log.info("文件已上传到 MinIO: bucket={}, object={}, size={}",
                    bucket, objectName, file.getSize());

            return buildUrl(objectName);
        } catch (Exception e) {
            log.error("MinIO 上传失败: bucket={}, object={}", bucket, objectName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String objectName = extractObjectName(fileUrl);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            log.info("文件已从 MinIO 删除: object={}", objectName);
        } catch (Exception e) {
            log.error("MinIO 删除失败: url={}", fileUrl, e);
        }
    }

    private String buildUrl(String objectName) {
        return publicUrl + "/" + bucket + "/" + objectName;
    }

    String extractObjectName(String fileUrl) {
        // URL 格式: http(s)://host:port/bucket/subdir/uuid.ext
        // 跳过 protocol://host:port/ 部分
        int afterScheme = fileUrl.indexOf("://");
        if (afterScheme < 0) return fileUrl;
        int firstSlash = fileUrl.indexOf('/', afterScheme + 3);
        if (firstSlash < 0) return fileUrl;
        int afterBucketSlash = fileUrl.indexOf('/', firstSlash + 1);
        if (afterBucketSlash < 0) return fileUrl;
        return fileUrl.substring(afterBucketSlash + 1);
    }

    private String extractExtension(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx > 0) {
            return filename.substring(dotIdx).toLowerCase();
        }
        return "";
    }

    private String detectContentType(String ext) {
        return switch (ext) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".svg" -> "image/svg+xml";
            default -> "application/octet-stream";
        };
    }
}
