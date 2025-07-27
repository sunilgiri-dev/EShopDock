package com.itransform.apigateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {
    private static final Logger logger = Logger.getLogger(JwtAuthFilter.class.getName());
    
    @Value("${jwt.secret:verySecureRandomKeyForDevelopmentOnly}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = String.valueOf(exchange.getRequest().getMethod());

        // Allow open endpoints
        if (path.startsWith("/api/v1/auth") || path.contains("/swagger-ui") || path.contains("/v3/api-docs")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return addErrorMessage(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SignatureException e) {
            logger.log(Level.WARNING, "Invalid JWT signature: {0}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return addErrorMessage(exchange, "Invalid JWT signature");
        } catch (MalformedJwtException e) {
            logger.log(Level.WARNING, "Invalid JWT token: {0}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return addErrorMessage(exchange, "Invalid JWT token format");
        } catch (ExpiredJwtException e) {
            logger.log(Level.WARNING, "JWT token is expired: {0}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return addErrorMessage(exchange, "JWT token has expired");
        } catch (UnsupportedJwtException e) {
            logger.log(Level.WARNING, "JWT token is unsupported: {0}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return addErrorMessage(exchange, "Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "JWT claims string is empty: {0}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return addErrorMessage(exchange, "JWT claims string is empty");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error processing JWT token: {0}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return addErrorMessage(exchange, "Error processing authentication token");
        }
        String role = claims.get("role", String.class);

        // --- RBAC matrix ---
        // Products
        if (path.startsWith("/api/v1/products")) {
            if ("GET".equals(method)) {
                // All roles can view
                return chain.filter(exchange);
            } else if (Set.of("POST", "PUT", "DELETE").contains(method)) {
                if ("ADMIN".equals(role) || "MANAGER".equals(role)) return chain.filter(exchange);
            }
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return addErrorMessage(exchange, "Access denied: Only ADMIN or MANAGER can modify products");
        }
        // Orders
        if (path.startsWith("/api/v1/orders")) {
            if ("GET".equals(method)) {
                // ADMIN, MANAGER, SUPPORT can view all; USER can view their own (handled in service)
                return chain.filter(exchange);
            } else if ("POST".equals(method)) {
                if (!"SUPPORT".equals(role)) return chain.filter(exchange); // USERS, ADMIN, MANAGER
            } else if (Set.of("PUT", "DELETE").contains(method)) {
                if ("ADMIN".equals(role) || "MANAGER".equals(role)) return chain.filter(exchange);
            }
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return addErrorMessage(exchange, "Access denied: Insufficient permissions for order operation");
        }
        // Inventory
        if (path.startsWith("/api/v1/inventory")) {
            if ("GET".equals(method)) {
                // Allow ADMIN, MANAGER, SUPPORT to access inventory data
                if ("ADMIN".equals(role) || "MANAGER".equals(role) || "SUPPORT".equals(role)) {
                    return chain.filter(exchange);
                }
            } else if (Set.of("POST", "PUT").contains(method)) {
                if ("ADMIN".equals(role) || "MANAGER".equals(role)) {
                    return chain.filter(exchange);
                }
            }
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return addErrorMessage(exchange, "Access denied: Insufficient permissions for inventory operations");
        }
        // Users (admin management)
        if (path.startsWith("/api/v1/users")) {
            if ("GET".equals(method)) {
                if ("ADMIN".equals(role) || "SUPPORT".equals(role)) return chain.filter(exchange);
            } else if (Set.of("POST", "PUT", "DELETE").contains(method)) {
                if ("ADMIN".equals(role)) return chain.filter(exchange);
            }
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return addErrorMessage(exchange, "Access denied: Only ADMIN can manage users");
        }

        // Default: Forbidden
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return addErrorMessage(exchange, "Access denied: Endpoint not accessible");
    }

    @Override
    public int getOrder() {
        return -1; // High priority
    }
    
    private Mono<Void> addErrorMessage(ServerWebExchange exchange, String message) {
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String errorJson = String.format("{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
                java.time.LocalDateTime.now().toString(),
                HttpStatus.UNAUTHORIZED.value(),
                message,
                exchange.getRequest().getURI().getPath());
        
        byte[] bytes = errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        org.springframework.core.io.buffer.DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(reactor.core.publisher.Mono.just(buffer));
    }
}
