package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("`order`")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status; // 0=pending_payment, 1=pending_delivery, 2=pending_receipt, 3=completed, 4=cancelled, 5=refunding
    private String paymentMethod;
    private LocalDateTime paymentTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    @TableField(exist = false)
    private List<OrderItem> items;
}
