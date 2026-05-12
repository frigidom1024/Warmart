package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;
    private String imageUrl;
    private Integer status;
    private LocalDateTime createdTime;

    @TableField(exist = false)
    private List<Category> children;
}
