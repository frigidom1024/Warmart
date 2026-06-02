package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("logistics_track")
public class LogisticsTrack {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String status; // ORDERED, WAREHOUSE, IN_TRANSIT, PICKUP, DELIVERED
    private String message;
    private String location;
    private LocalDateTime trackTime;
    private LocalDateTime createdTime;
}
