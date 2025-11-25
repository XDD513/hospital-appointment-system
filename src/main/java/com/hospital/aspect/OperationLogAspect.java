package com.hospital.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.annotation.OperationLog;
import com.hospital.service.SystemService;
import com.hospital.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志AOP切面
 * 自动记录带有@OperationLog注解的方法的操作日志
 *
 * @author Hospital System
 * @since 2025-11-06
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private SystemService systemService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 定义切点：所有带有@OperationLog注解的方法
     */
    @Pointcut("@annotation(com.hospital.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    /**
     * 环绕通知：记录操作日志
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 创建日志对象
        com.hospital.entity.OperationLog operationLog = new com.hospital.entity.OperationLog();

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        if (annotation != null) {
            operationLog.setOperationModule(annotation.module());
            operationLog.setOperationType(annotation.type());
            operationLog.setOperationDesc(annotation.description());
        }

        // 获取用户信息
        if (request != null) {
            try {
                String token = jwtUtil.getTokenFromRequest(request);
                if (token != null && jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserId(token);
                    String username = jwtUtil.getUsername(token);
                    operationLog.setUserId(userId);
                    operationLog.setUsername(username);
                }
            } catch (Exception e) {
                log.warn("获取用户信息失败: {}", e.getMessage());
            }

            // 获取请求信息
            operationLog.setRequestMethod(request.getMethod());
            operationLog.setRequestUrl(request.getRequestURI());
            operationLog.setIpAddress(getIpAddress(request));
            operationLog.setUserAgent(request.getHeader("User-Agent"));

            Object[] args = joinPoint.getArgs();

            // 登录特殊处理：若未从token获取到用户名，则尝试从请求体中解析
            if ((operationLog.getUsername() == null || operationLog.getUsername().isEmpty()) && args != null) {
                for (Object arg : args) {
                    if (arg instanceof com.hospital.dto.request.LoginRequest) {
                        com.hospital.dto.request.LoginRequest loginRequest = (com.hospital.dto.request.LoginRequest) arg;
                        operationLog.setUsername(loginRequest.getUsername());
                        break;
                    }
                }
            }

            // 获取请求参数
            try {
                if (args != null && args.length > 0) {
                    // 过滤掉HttpServletRequest等不需要序列化的参数，并处理MultipartFile
                    Object[] filteredArgs = new Object[args.length];
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof HttpServletRequest ||
                            args[i] instanceof javax.servlet.http.HttpServletResponse) {
                            filteredArgs[i] = null;
                        } else if (args[i] instanceof MultipartFile) {
                            // 将MultipartFile转换为可序列化的Map
                            MultipartFile file = (MultipartFile) args[i];
                            Map<String, Object> fileInfo = new HashMap<>();
                            fileInfo.put("originalFilename", file.getOriginalFilename());
                            fileInfo.put("size", file.getSize());
                            fileInfo.put("contentType", file.getContentType());
                            fileInfo.put("name", file.getName());
                            filteredArgs[i] = fileInfo;
                        } else {
                            filteredArgs[i] = args[i];
                        }
                    }
                    String params = objectMapper.writeValueAsString(filteredArgs);
                    // 限制参数长度
                    if (params.length() > 2000) {
                        params = params.substring(0, 2000) + "...";
                    }
                    operationLog.setRequestParams(params);
                }
            } catch (Exception e) {
                log.warn("序列化请求参数失败: {}", e.getMessage());
                operationLog.setRequestParams("参数序列化失败");
            }
        }

        // 执行目标方法
        Object result = null;
        try {
            result = joinPoint.proceed();
            operationLog.setStatus(1); // 成功

            // 登录成功后补充用户名/用户ID
            if ((operationLog.getUsername() == null || operationLog.getUsername().isEmpty())
                    || operationLog.getUserId() == null) {
                if (result instanceof com.hospital.common.result.Result) {
                    com.hospital.common.result.Result<?> resp = (com.hospital.common.result.Result<?>) result;
                    Object data = resp.getData();
                    if (data instanceof com.hospital.dto.response.LoginResponse) {
                        com.hospital.dto.response.LoginResponse loginResp = (com.hospital.dto.response.LoginResponse) data;
                        if (operationLog.getUsername() == null || operationLog.getUsername().isEmpty()) {
                            operationLog.setUsername(loginResp.getUsername());
                        }
                        if (operationLog.getUserId() == null && loginResp.getId() != null) {
                            operationLog.setUserId(loginResp.getId());
                        }
                    } else if (data instanceof Map) {
                        Map<?, ?> mapData = (Map<?, ?>) data;
                        if ((operationLog.getUsername() == null || operationLog.getUsername().isEmpty()) && mapData.get("username") != null) {
                            operationLog.setUsername(String.valueOf(mapData.get("username")));
                        }
                        if (operationLog.getUserId() == null && mapData.get("id") != null) {
                            try {
                                operationLog.setUserId(Long.valueOf(String.valueOf(mapData.get("id"))));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }

            // 记录响应数据
            try {
                String responseData = objectMapper.writeValueAsString(result);
                // 限制响应数据长度
                if (responseData.length() > 2000) {
                    responseData = responseData.substring(0, 2000) + "...";
                }
                operationLog.setResponseData(responseData);
            } catch (Exception e) {
                log.warn("序列化响应数据失败: {}", e.getMessage());
                operationLog.setResponseData("响应序列化失败");
            }

        } catch (Throwable e) {
            operationLog.setStatus(0); // 失败
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500) + "...";
            }
            operationLog.setErrorMsg(errorMsg);
            throw e; // 继续抛出异常
        } finally {
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            operationLog.setExecutionTime((int) executionTime);
            operationLog.setCreatedAt(LocalDateTime.now());

            // 异步保存日志（避免影响主业务）
            try {
                systemService.recordOperationLog(operationLog);
            } catch (Exception e) {
                log.error("保存操作日志失败: {}", e.getMessage(), e);
            }
        }

        return result;
    }

    /**
     * 获取客户端IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

