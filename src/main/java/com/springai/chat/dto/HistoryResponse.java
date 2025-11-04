package com.springai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 会话历史响应
 * 
 * @author yanwenjie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryResponse {
    
    private String conversationId;
    private Integer totalCount;
    private Integer returnedCount;
    private Long remainingTtl;
    private List<Message> messages;
}

