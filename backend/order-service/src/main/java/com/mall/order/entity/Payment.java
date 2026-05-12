package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private BigDecimal amount;
    private String method;
    private Integer status; // 0=pending, 1=paid, 2=refunded
    private LocalDateTime payTime;
}
