package com.hospital.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * 根据握手阶段保存的信息创建用户Principal。
 */
@Component
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object userIdObj = attributes.get("userId");
        if (userIdObj == null) {
            return () -> UUID.randomUUID().toString();
        }
        String name = String.valueOf(userIdObj);
        return () -> name;
    }
}

