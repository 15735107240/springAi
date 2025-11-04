package com.springai.common.exception;

import com.springai.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一处理所有模块的异常
 * 
 * @author yanwenjie
 */
@Slf4j
@RestControllerAdvice(basePackages = {"com.springai.auth.controller", "com.springai.chat.controller"})
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        ErrorResponse response = new ErrorResponse("业务异常", e.getMessage(), e.getCode());
        HttpStatus status = HttpStatus.resolve(e.getCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("参数校验异常: {}", e.getMessage());
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        ErrorResponse response = new ErrorResponse("参数校验失败", message, 400);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数异常: {}", e.getMessage());
        ErrorResponse response = new ErrorResponse("参数错误", e.getMessage(), 400);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理非法状态异常（如验证码发送过于频繁）
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.warn("非法状态异常: {}", e.getMessage());
        ErrorResponse response = new ErrorResponse("状态错误", e.getMessage(), 400);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("系统异常", e);
        ErrorResponse response = new ErrorResponse("系统错误", "系统错误，请稍后重试", 500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

