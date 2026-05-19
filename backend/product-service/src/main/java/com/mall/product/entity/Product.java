package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    @TableField("original_price")
    private BigDecimal originalPrice;
    private String tag;
    private Integer stock;
    private Integer sales;
    @TableField("image")
    private String mainImage;
    private Integer status;
    @TableField("is_recommended")
    private Integer isRecommend;
    @TableField(exist = false)
    private Integer hasSpec;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @TableField(exist = false)
    private List<ProductSpec> specList;

    @TableField(exist = false)
    private List<ProductImage> imageList;
}
