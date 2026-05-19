package com.mall.product.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long productId;
    private Long userId;
    private String content;
    private Integer rating;
    private String imageUrls;
    private LocalDateTime createdTime;

    private String userNickname;
    private String userAvatar;
}
