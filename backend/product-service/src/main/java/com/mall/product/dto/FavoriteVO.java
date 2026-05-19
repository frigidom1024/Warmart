package com.mall.product.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FavoriteVO {
    private Long id;
    private Long userId;
    private Long productId;
    private LocalDateTime createdTime;

    private String productName;
    private BigDecimal productPrice;
    private BigDecimal productOldPrice;
    private String productImage;
    private String productTag;
    private Integer productSales;
}
