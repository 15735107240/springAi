package com.springai.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一错误响应
 * 
 * @author yanwenjie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private String error;
    private String message;
    private Integer code;
    
    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}

