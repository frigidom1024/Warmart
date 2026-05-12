package com.mall.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("consultation")
public class Consultation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private String question;
    private String answer;
    private Integer status; // 0=pending, 1=replied
    private LocalDateTime createdTime;
}
