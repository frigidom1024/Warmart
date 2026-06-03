package com.mall.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    @TableField("email_verified")
    private Boolean emailVerified;
    private String avatar;
    private String role; // USER, ADMIN, SUPER_ADMIN
    private Integer status; // 0=normal, 1=disabled
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
