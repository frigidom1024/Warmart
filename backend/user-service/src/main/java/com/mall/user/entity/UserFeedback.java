package com.mall.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_feedback")
public class UserFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type; // complaint, suggestion
    private String content;
    private String reply;
    private LocalDateTime replyTime;
    private Integer status; // 0=pending, 1=replied
    private LocalDateTime createdTime;
}
