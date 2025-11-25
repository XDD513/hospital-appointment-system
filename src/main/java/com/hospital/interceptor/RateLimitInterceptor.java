package com.hospital.interceptor;

import com.hospital.annotation.RateLimit;
import com.hospital.common.exception.BusinessException;
import com.hospital.common.result.ResultCode;
import com.hospital.service.RateLimitService;
import com.hospital.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 接口限流拦截器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }
        if (rateLimit == null) {
            return true;
        }

        String key = buildLimitKey(rateLimit, request);
        boolean allowed = rateLimitService.tryAcquire(key, rateLimit.limit(), rateLimit.windowSeconds());
        if (!allowed) {
            throw new BusinessException(ResultCode.FORBIDDEN, "请求过于频繁，请稍后再试");
        }
        return true;
    }

    private String buildLimitKey(RateLimit rateLimit, HttpServletRequest request) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(rateLimit.key())) {
            builder.append(rateLimit.key());
        } else {
            builder.append(request.getServletPath());
        }

        if (rateLimit.perIp()) {
            builder.append(":ip:").append(resolveClientIp(request));
        }

        if (rateLimit.perUser()) {
            Long userId = jwtUtil.getUserIdFromRequest(request);
            if (userId != null) {
                builder.append(":uid:").append(userId);
            }
        }
        return builder.toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String[] headerCandidates = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headerCandidates) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}


