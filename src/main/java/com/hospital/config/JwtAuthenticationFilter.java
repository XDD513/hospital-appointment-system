package com.hospital.config;

import com.hospital.common.constant.SystemSettingKeys;
import com.hospital.config.SystemSettingManager;
import com.hospital.util.JwtUtil;
import com.hospital.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SystemSettingManager systemSettingManager;

    // 会话过期时间（秒），用于滑动过期，配置项 hospital.auth.token-ttl-seconds
    @Value("${hospital.auth.token-ttl-seconds:7200}")
    private long tokenTtlSeconds;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS请求直接放行
        if ("OPTIONS".equals(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 登录注册接口不需要验证
        if (requestURI.contains("/login") || requestURI.contains("/register") ||
            requestURI.contains("/user/check") || requestURI.contains("/swagger") ||
            requestURI.contains("/v3/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // WebSocket相关路径不需要JWT验证（SockJS的info端点等）
        if (requestURI.startsWith("/ws/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取token
        String token = jwtUtil.getTokenFromRequest(request);

        // 如果没有token或token无效，直接返回401
        if (token == null || !jwtUtil.validateToken(token)) {
            log.warn("Token无效或不存在，URI：{}", requestURI);
            sendUnauthorizedResponse(response, "认证令牌无效或已过期");
            return;
        }

        // 从token中获取用户信息
        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        Integer roleType = jwtUtil.getRoleType(token);

        if (userId == null || username == null) {
            log.warn("无法从token中解析用户信息，URI：{}", requestURI);
            sendUnauthorizedResponse(response, "认证令牌无效");
            return;
        }

        // ================== Redis 会话校验与滑动续期（中文注释） ==================
        // 1) 校验Redis中是否存在会话键，若不存在则视为登录超时或未登录，返回401
        // 2) 若存在，则将该会话的TTL续期，实现滑动过期
        String tokenKey = "hospital:auth:token:" + token;
        Object session = null;
        try {
            session = redisUtil.get(tokenKey);
        } catch (Exception e) {
            log.warn("读取Redis会话失败: tokenKey={}, error={}", tokenKey.substring(0, Math.min(30, tokenKey.length())) + "...", e.getMessage());
        }

        if (session == null) {
            log.warn("Redis会话不存在，可能登录已过期或被其他设备登录挤下线，URI：{}，tokenKey前缀：{}", requestURI, tokenKey.substring(0, Math.min(30, tokenKey.length())) + "...");
            sendUnauthorizedResponse(response, "登录已过期，请重新登录");
            return;
        }

        // ================== 单设备登录校验（中文注释） ==================
        // 验证当前token是否是该用户的最新token，确保单设备登录
        // 如果不是最新token，则说明用户在其他设备登录，当前token应该失效
        String userTokenKey = "hospital:auth:user:" + userId + ":token";
        Object currentValidTokenObj = null;
        try {
            currentValidTokenObj = redisUtil.get(userTokenKey);
        } catch (Exception e) {
            log.warn("读取用户最新token失败: userId={}, error={}", userId, e.getMessage());
        }

        if (currentValidTokenObj == null) {
            // 如果用户没有记录最新token，说明可能已退出登录，返回401
            log.warn("用户最新token记录不存在，可能已退出登录或被挤下线，userId={}, URI：{}", userId, requestURI);
            sendUnauthorizedResponse(response, "登录已过期，请重新登录");
            return;
        }

        String currentValidToken = String.valueOf(currentValidTokenObj);
        if (!currentValidToken.equals(token)) {
            // 当前token不是最新token，说明用户在其他设备登录，当前token已失效
            log.warn("用户在其他设备登录，当前token已失效，userId={}, URI：{}", userId, requestURI);
            // 删除已失效的会话信息
            try {
                redisUtil.delete(tokenKey);
            } catch (Exception e) {
                log.warn("删除失效token会话失败: tokenKey={}, error={}", tokenKey, e.getMessage());
            }
            sendUnauthorizedResponse(response, "登录已过期，请重新登录");
            return;
        }

        // 滑动续期：刷新会话TTL
        try {
            long sessionTtl = resolveSessionTimeoutSeconds();
            redisUtil.expire(tokenKey, sessionTtl, TimeUnit.SECONDS);
            // 同时刷新用户最新token记录的TTL
            redisUtil.expire(userTokenKey, sessionTtl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("刷新Redis会话TTL失败: tokenKey={}, error={}", tokenKey, e.getMessage());
        }

        // 创建认证对象
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        
        // 将用户信息存储到SecurityContext中
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 将用户信息放入请求属性中
        request.setAttribute("userId", userId);
        request.setAttribute("roleType", roleType);

        filterChain.doFilter(request, response);
    }

    private long resolveSessionTimeoutSeconds() {
        Integer minutes = systemSettingManager.getInteger(SystemSettingKeys.SECURITY_SESSION_TIMEOUT, (int) (tokenTtlSeconds / 60));
        if (minutes == null || minutes <= 0) {
            return tokenTtlSeconds;
        }
        return minutes * 60L;
    }

    /**
     * 发送401未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
        response.getWriter().flush();
    }
}
