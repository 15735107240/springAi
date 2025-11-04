package com.springai.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送验证码响应
 * 
 * @author yanwenjie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendCodeResponse {
    
    private boolean success;
    private String message;
    private String phoneNumber;
    private String code; // 仅用于测试，生产环境应删除
}

