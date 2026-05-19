package com.mall.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartVO {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private Integer checked;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    private String productName;
    private BigDecimal productPrice;
    private BigDecimal productOldPrice;
    private String productImage;
    private String productTag;
}
