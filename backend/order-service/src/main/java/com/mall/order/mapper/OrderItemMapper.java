package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("SELECT product_id AS productId, product_name AS productName, product_image AS productImage, SUM(quantity) AS sales, SUM(subtotal) AS amount FROM order_item GROUP BY product_id, product_name, product_image ORDER BY sales DESC LIMIT #{limit}")
    List<Map<String, Object>> hotProducts(int limit);
}
