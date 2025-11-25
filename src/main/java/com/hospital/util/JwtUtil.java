package com.hospital.util;

import com.hospital.common.constant.SystemConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {
    
    /**
     * 生成JWT令牌
     */
    public String generateToken(Long userId, String username, Integer roleType) {
        Date expirationDate = new Date(System.currentTimeMillis() + SystemConstants.getJwtExpiration());
        
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("roleType", roleType)
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(Keys.hmacShaKeyFor(SystemConstants.getJwtSecret().getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * 解析JWT令牌
     */
    public Claims parseToken(String token) {
        try {
            // 检查token格式
            if (token == null || token.trim().isEmpty()) {
                log.error("JWT token为空");
                return null;
            }
            
            // 检查token是否包含三个部分（header.payload.signature）
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.error("JWT token格式不正确，应该有3个部分，实际有{}个部分", parts.length);
                return null;
            }
            
            return Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(SystemConstants.getJwtSecret().getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("JWT解析失败：{}", e.getMessage());
            log.error("Token内容：{}", token);
            return null;
        }
    }
    
    /**
     * 验证JWT令牌
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                return false;
            }
            
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            log.error("JWT验证失败：{}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 从令牌获取用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("userId", Long.class);
        }
        return null;
    }

    /**
     * 从令牌获取用户ID（别名方法）
     */
    public Long getUserIdFromToken(String token) {
        return getUserId(token);
    }

    /**
     * 从令牌获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.getSubject();
        }
        return null;
    }
    
    /**
     * 从令牌获取角色类型
     */
    public Integer getRoleType(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("roleType", Integer.class);
        }
        return null;
    }
    
    /**
     * 从请求头获取令牌
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(SystemConstants.TOKEN_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(SystemConstants.TOKEN_PREFIX)) {
            return authHeader.substring(SystemConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
    
    /**
     * 从请求获取用户ID
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token != null) {
            return getUserId(token);
        }
        return null;
    }
    
    /**
     * 从请求获取角色类型
     */
    public Integer getRoleTypeFromRequest(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        if (token != null) {
            return getRoleType(token);
        }
        return null;
    }
}