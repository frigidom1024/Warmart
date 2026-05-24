package com.mall.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** Access token expiry in seconds (default: 1 day) */
    private long accessTokenExp = 86400;
    /** Refresh token expiry in seconds (default: 30 days) */
    private long refreshTokenExp = 2592000;
}
