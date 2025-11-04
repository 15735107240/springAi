package com.springai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 所有会话响应
 * 
 * @author yanwenjie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllConversationsResponse {
    
    private Integer total;
    private List<ConversationDetail> conversations;
}

