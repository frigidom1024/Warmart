package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private Integer sales;
    private String mainImage;
    private Integer status;
    private Integer isRecommend;
    private Integer hasSpec;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
