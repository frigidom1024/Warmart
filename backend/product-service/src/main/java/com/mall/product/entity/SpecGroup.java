package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.List;

@Data
@TableName("product_spec_group")
public class SpecGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String name;
    private Integer sort;
    @TableField(exist = false)
    private List<SpecValue> values;
}
