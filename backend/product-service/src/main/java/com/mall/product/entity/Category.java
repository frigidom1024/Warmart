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
    @TableField("icon")
    private String imageUrl;
    @TableField(exist = false)
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @TableField(exist = false)
    private List<Category> children;
}
