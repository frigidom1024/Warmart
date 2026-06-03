package com.mall.product.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象接口，用于将上传文件保存到对象存储（MinIO）或其它存储后端。
 */
public interface FileStorageService {

    /**
     * 上传文件到指定子目录。
     *
     * @param file        上传的文件
     * @param subdirectory 存储子目录（如 "products", "comments", "categories"）
     * @return 文件的完整可访问 URL
     */
    String uploadFile(MultipartFile file, String subdirectory);

    /**
     * 根据完整 URL 删除文件。
     *
     * @param fileUrl 文件的完整 URL
     */
    void deleteFile(String fileUrl);
}
