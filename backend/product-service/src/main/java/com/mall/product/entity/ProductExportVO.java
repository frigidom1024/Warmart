package com.mall.product.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class ProductExportVO {

    @ExcelProperty("商品名称")
    @ColumnWidth(30)
    private String name;

    @ExcelProperty("分类")
    @ColumnWidth(16)
    private String categoryName;

    @ExcelProperty("价格")
    @ColumnWidth(12)
    private Double price;

    @ExcelProperty("原价")
    @ColumnWidth(12)
    private Double originalPrice;

    @ExcelProperty("库存")
    @ColumnWidth(10)
    private Integer stock;

    @ExcelProperty("销量")
    @ColumnWidth(10)
    private Integer sales;

    @ExcelProperty("商品描述")
    @ColumnWidth(40)
    private String description;

    @ExcelProperty("主图URL")
    @ColumnWidth(40)
    private String mainImage;

    @ExcelProperty("标签")
    @ColumnWidth(10)
    private String tag;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusText;

    @ExcelProperty("是否推荐")
    @ColumnWidth(12)
    private String recommendText;
}
