package com.springai.chat.controller;

import com.springai.chat.dto.AllConversationsResponse;
import com.springai.chat.dto.ApiResponse;
import com.springai.chat.dto.BatchCheckResponse;
import com.springai.chat.dto.ConversationDetail;
import com.springai.chat.dto.ConversationInfoResponse;
import com.springai.chat.dto.HistoryResponse;
import com.springai.chat.dto.PageHistoryResponse;
import com.springai.chat.dto.RefreshTtlResponse;
import com.springai.common.exception.BusinessException;
import com.springai.chat.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话历史管理控制器
 * 提供会话历史的查询、清除等功能
 * 
 * @author yanwenjie
 * @create 2025-10-30
 */
@Slf4j
@RestController
@RequestMapping("/api/history")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;
    
    @Value("${chat.admin.key:admin}")
    private String adminKey;

    public ChatHistoryController(ChatHistoryService chatHistoryService) {
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * 获取会话历史
     * @param conversationId 会话ID
     * @param lastN 获取最近N条消息，不传则获取全部
     * @return 消息列表
     */
    @GetMapping("/{conversationId}")
    public HistoryResponse getHistory(
            @PathVariable String conversationId,
            @RequestParam(required = false, defaultValue = "-1") int lastN) {
        
        log.info("查询会话历史: conversationId={}, lastN={}", conversationId, lastN);
        
        List<Message> messages = chatHistoryService.getHistory(conversationId, lastN);
        int totalCount = chatHistoryService.getMessageCount(conversationId);
        long remainingTtl = chatHistoryService.getRemainingTtl(conversationId);
        
        return HistoryResponse.builder()
                .conversationId(conversationId)
                .totalCount(totalCount)
                .returnedCount(messages.size())
                .remainingTtl(remainingTtl)
                .messages(messages)
                .build();
    }

    /**
     * 清除会话历史
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/{conversationId}")
    public ApiResponse<String> clearHistory(@PathVariable String conversationId) {
        log.info("清除会话历史: conversationId={}", conversationId);
        
        chatHistoryService.clearHistory(conversationId);
        
        return ApiResponse.success("会话历史已清除", conversationId);
    }

    /**
     * 获取会话信息
     * @param conversationId 会话ID
     * @return 会话信息
     */
    @GetMapping("/{conversationId}/info")
    public ConversationInfoResponse getConversationInfo(@PathVariable String conversationId) {
        log.info("查询会话信息: conversationId={}", conversationId);
        
        boolean exists = chatHistoryService.exists(conversationId);
        int messageCount = chatHistoryService.getMessageCount(conversationId);
        long remainingTtl = chatHistoryService.getRemainingTtl(conversationId);
        
        return ConversationInfoResponse.builder()
                .conversationId(conversationId)
                .exists(exists)
                .messageCount(messageCount)
                .remainingTtl(remainingTtl)
                .remainingTtlHours(remainingTtl > 0 ? remainingTtl / 3600.0 : 0)
                .build();
    }

    /**
     * 刷新会话过期时间
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @PostMapping("/{conversationId}/refresh")
    public ApiResponse<RefreshTtlResponse> refreshTtl(@PathVariable String conversationId) {
        log.info("刷新会话过期时间: conversationId={}", conversationId);
        
        chatHistoryService.refreshTtl(conversationId);
        long remainingTtl = chatHistoryService.getRemainingTtl(conversationId);
        
        RefreshTtlResponse data = RefreshTtlResponse.builder()
                .conversationId(conversationId)
                .remainingTtl(remainingTtl)
                .build();
        
        return ApiResponse.success("会话过期时间已刷新", data);
    }

    /**
     * 批量检查会话是否存在
     * @param conversationIds 会话ID列表
     * @return 各会话的存在状态
     */
    @PostMapping("/batch/check")
    public BatchCheckResponse batchCheckExists(@RequestBody List<String> conversationIds) {
        log.info("批量检查会话存在性: {}", conversationIds);
        
        Map<String, Boolean> existsMap = new HashMap<>();
        for (String conversationId : conversationIds) {
            existsMap.put(conversationId, chatHistoryService.exists(conversationId));
        }
        
        return BatchCheckResponse.builder()
                .total(conversationIds.size())
                .exists(existsMap)
                .build();
    }

    /**
     * 分页查询会话历史
     * @param conversationId 会话ID
     * @param page 页码（从1开始，默认1）
     * @param size 每页大小（默认10）
     * @return 分页结果
     */
    @GetMapping("/{conversationId}/page")
    public PageHistoryResponse getHistoryByPage(
            @PathVariable String conversationId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        log.info("分页查询会话历史: conversationId={}, page={}, size={}", conversationId, page, size);
        
        // 参数验证
        if (page < 1) {
            throw new BusinessException("页码不能小于1");
        }
        
        if (size <= 0 || size > 100) {
            throw new BusinessException("每页大小必须在1-100之间");
        }
        
        // 转换为从0开始的页码（内部使用）
        int internalPage = page - 1;
        ChatHistoryService.PageResult pageResult = chatHistoryService.getMessagesByPage(conversationId, internalPage, size);
        
        return PageHistoryResponse.builder()
                .conversationId(conversationId)
                .page(page)  // 返回用户传入的页码（从1开始）
                .size(pageResult.getSize())
                .totalMessages(pageResult.getTotalMessages())
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.isHasNext())
                .hasPrevious(pageResult.isHasPrevious())
                .messages(pageResult.getMessages())
                .build();
    }

    /**
     * 获取所有会话ID列表（管理员接口）
     * @param adminKey 管理员密钥
     * @return 所有会话ID列表
     */
    @GetMapping("/admin/conversations")
    public AllConversationsResponse getAllConversations(
            @RequestParam(required = false) String adminKey) {
        
        log.info("管理员查询所有会话列表");
        
        // 验证管理员权限
        if (adminKey == null || !adminKey.equals(this.adminKey)) {
            log.warn("管理员接口访问被拒绝，提供的密钥: {}", adminKey != null ? "***" : "null");
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), "需要管理员权限才能访问此接口");
        }
        
        List<String> conversationIds = chatHistoryService.getAllConversationIds();
        
        // 获取每个会话的详细信息
        List<ConversationDetail> conversationDetails = conversationIds.stream()
                .map(id -> ConversationDetail.builder()
                        .conversationId(id)
                        .messageCount(chatHistoryService.getMessageCount(id))
                        .remainingTtl(chatHistoryService.getRemainingTtl(id))
                        .build())
                .toList();
        
        return AllConversationsResponse.builder()
                .total(conversationIds.size())
                .conversations(conversationDetails)
                .build();
    }
}

