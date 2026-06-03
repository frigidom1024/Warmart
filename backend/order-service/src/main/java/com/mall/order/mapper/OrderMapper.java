package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM `order` WHERE status = 3")
    BigDecimal sumTotalSales();

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM `order` WHERE status = 3 AND created_time >= #{start} AND created_time < #{end}")
    BigDecimal sumSalesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM `order` WHERE created_time >= #{start} AND created_time < #{end}")
    Long countOrdersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT status, COUNT(*) AS `count` FROM `order` GROUP BY status ORDER BY status")
    List<Map<String, Object>> countByStatus();

    @Select("SELECT DATE(created_time) AS `date`, COUNT(*) AS order_count, COALESCE(SUM(CASE WHEN status = 3 THEN total_amount ELSE 0 END), 0) AS sales_amount FROM `order` WHERE created_time >= #{since} GROUP BY DATE(created_time) ORDER BY `date`")
    List<Map<String, Object>> dailySalesSince(@Param("since") LocalDateTime since);
}
