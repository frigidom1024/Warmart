package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("product_spec_value")
public class SpecValue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private String value;
    private Integer sort;
}
