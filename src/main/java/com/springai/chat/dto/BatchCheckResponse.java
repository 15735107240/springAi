package com.springai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 批量检查响应
 * 
 * @author yanwenjie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCheckResponse {
    
    private Integer total;
    private Map<String, Boolean> exists;
}

