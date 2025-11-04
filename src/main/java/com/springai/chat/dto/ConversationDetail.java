package com.springai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话详情
 * 
 * @author yanwenjie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDetail {
    
    private String conversationId;
    private Integer messageCount;
    private Long remainingTtl;
}

