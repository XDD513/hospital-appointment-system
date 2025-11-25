package com.hospital.interceptor;

import com.hospital.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT拦截器
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // OPTIONS请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        // 登录注册接口不需要验证
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/login") || requestURI.contains("/register") || 
            requestURI.contains("/check") || requestURI.contains("/swagger") ||
            requestURI.contains("/v3/api-docs")) {
            return true;
        }
        
        // WebSocket相关路径不需要JWT验证（SockJS的info端点等）
        if (requestURI.startsWith("/ws/")) {
            return true;
        }
        
        // 获取token
        String token = jwtUtil.getTokenFromRequest(request);
        if (token == null) {
            log.warn("请求缺少Authorization头，URI：{}", requestURI);
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"缺少认证令牌\"}");
            return false;
        }
        
        // 验证token
        if (!jwtUtil.validateToken(token)) {
            log.warn("JWT令牌验证失败，URI：{}", requestURI);
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"认证令牌无效\"}");
            return false;
        }
        
        // 将用户信息放入请求属性中
        Long userId = jwtUtil.getUserId(token);
        Integer roleType = jwtUtil.getRoleType(token);
        request.setAttribute("userId", userId);
        request.setAttribute("roleType", roleType);
        
        return true;
    }
}
