package com.springai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话信息响应
 *
 * @author yanwenjie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationInfoResponse {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 是否存在
     */
    private boolean exists;

    /**
     * 消息数量
     */
    private int messageCount;

    /**
     * 剩余过期时间（秒）
     */
    private long remainingTtl;

    /**
     * 剩余过期时间（小时）
     */
    private double remainingTtlHours;
}

