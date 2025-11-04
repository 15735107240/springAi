package com.springai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新TTL响应
 * 
 * @author yanwenjie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTtlResponse {
    
    private String conversationId;
    private Long remainingTtl;
}

