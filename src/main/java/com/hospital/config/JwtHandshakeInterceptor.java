package com.hospital.config;

import com.hospital.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * WebSocket握手拦截器，负责从请求中解析JWT并写入用户信息。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) {
            log.warn("非Servlet请求，拒绝握手");
            return false;
        }

        HttpServletRequest httpRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String token = httpRequest.getParameter("token");

        if (!StringUtils.hasText(token)) {
            String authHeader = httpRequest.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (!StringUtils.hasText(token)) {
            log.warn("WebSocket握手缺少token");
            return false;
        }

        if (!jwtUtil.validateToken(token)) {
            log.warn("WebSocket握手token无效");
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            log.warn("WebSocket握手未解析到用户ID");
            return false;
        }

        attributes.put("userId", userId);
        attributes.put("token", token);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }
}

