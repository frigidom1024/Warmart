package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_application")
public class RefundApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long userId;
    private String reason;
    private BigDecimal amount;
    private Integer previousStatus;
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED
    private String adminReply;
    private LocalDateTime handledTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @TableField(exist = false)
    private String orderNo;
    @TableField(exist = false)
    private String receiverName;
}
