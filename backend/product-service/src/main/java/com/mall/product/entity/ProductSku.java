package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@TableName("product_sku")
public class ProductSku {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String specValueIds;  // JSON array "[1,3]"

    @TableField(exist = false)
    private List<Long> specValueIdList;  // parsed from specValueIds for API

    private BigDecimal price;
    private Integer stock;
    private String image;
    private Boolean enabled;
    private Integer sort;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public void parseSpecValueIds() {
        if (specValueIds != null && specValueIdList == null) {
            try {
                specValueIdList = OBJECT_MAPPER.readValue(specValueIds,
                        new TypeReference<List<Long>>() {});
            } catch (JsonProcessingException e) {
                specValueIdList = List.of();
            }
        }
    }
}
