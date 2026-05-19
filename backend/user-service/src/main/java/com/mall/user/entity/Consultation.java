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
    @TableField("content")
    private String question;
    @TableField("reply_content")
    private String answer;
    private Integer status; // 0=pending, 1=replied
    private LocalDateTime createdTime;
}
