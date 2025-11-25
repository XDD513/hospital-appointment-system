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

/**
 * 全局异常处理器
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("业务异常: URI={}, Code={}, Message={}", 
                request.getRequestURI(), e.getCode(), e.getMessage());
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
        StringBuilder errorMessage = new StringBuilder();
        for (FieldError error : fieldErrors) {
            errorMessage.append(error.getField())
                    .append(": ")
                    .append(error.getDefaultMessage())
                    .append("; ");
        }
        log.error("{}: URI={}, Message={}", errorType, request.getRequestURI(), errorMessage);
        return Result.error(ResultCode.BAD_REQUEST.getCode(), errorMessage.toString());
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常: URI={}", request.getRequestURI(), e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(), "系统内部错误");
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: URI={}", request.getRequestURI(), e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(), "系统错误: " + e.getMessage());
    }
}

