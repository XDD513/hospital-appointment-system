package com.hospital.common.exception;

import com.hospital.common.result.Result;
import com.hospital.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

/**
 * 全局异常处理器
 * 支持业务场景分类与可观测日志字段（traceId）
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        log.error("业务异常 [traceId={}, method={}, uri={}, code={}, message={}]", 
                traceId, method, uri, e.getCode(), e.getMessage(), e);
        
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        return handleFieldErrors(e.getBindingResult().getFieldErrors(), request, "参数校验异常");
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e, HttpServletRequest request) {
        return handleFieldErrors(e.getBindingResult().getFieldErrors(), request, "参数绑定异常");
    }

    /**
     * 统一处理字段错误
     */
    private Result<Void> handleFieldErrors(List<FieldError> fieldErrors, HttpServletRequest request, String errorType) {
        String traceId = generateTraceId();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        StringBuilder errorMessage = new StringBuilder();
        for (FieldError error : fieldErrors) {
            errorMessage.append(error.getField())
                    .append(": ")
                    .append(error.getDefaultMessage())
                    .append("; ");
        }
        
        log.error("{} [traceId={}, method={}, uri={}, message={}]", 
                errorType, traceId, method, uri, errorMessage);
        
        return Result.error(ResultCode.BAD_REQUEST.getCode(), errorMessage.toString());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        log.error("空指针异常 [traceId={}, method={}, uri={}]", traceId, method, uri, e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(), "系统内部错误");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        String traceId = generateTraceId();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        log.error("非法参数异常 [traceId={}, method={}, uri={}, message={}]", 
                traceId, method, uri, e.getMessage(), e);
        
        return Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String traceId = generateTraceId();
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        log.error("系统异常 [traceId={}, method={}, uri={}, exception={}, message={}]", 
                traceId, method, uri, e.getClass().getSimpleName(), e.getMessage(), e);
        
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(), "系统错误，请稍后重试");
    }
}

