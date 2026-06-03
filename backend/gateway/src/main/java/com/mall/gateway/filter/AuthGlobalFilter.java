package com.mall.gateway.filter;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.text.ParseException;

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = extractToken(exchange.getRequest());
        if (token != null) {
            try {
                JWT jwt = JWTParser.parse(token);
                var claims = jwt.getJWTClaimsSet();
                String userId = claims.getSubject() != null ? claims.getSubject() : "";
                String role = claims.getStringClaim("role");

                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role != null ? role : "")
                        .build();

                // Check admin endpoint role authorization
                String path = modifiedRequest.getURI().getPath();
                if (path.contains("/admin/") && (role == null || (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)))) {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.FORBIDDEN);
                    return response.setComplete();
                }

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (ParseException e) {
                log.warn("Failed to parse JWT token: {}", e.getMessage());
            }
        }
        return chain.filter(exchange);
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
