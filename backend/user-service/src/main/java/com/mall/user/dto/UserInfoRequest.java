package com.mall.user.dto;

import lombok.Data;

@Data
public class UserInfoRequest {
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
}
