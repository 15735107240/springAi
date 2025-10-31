package com.springai.chat.controller;

import com.springai.chat.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    public ResponseEntity<Map<String, Object>> getHistory(
            @PathVariable String conversationId,
            @RequestParam(required = false, defaultValue = "-1") int lastN) {
        
        log.info("查询会话历史: conversationId={}, lastN={}", conversationId, lastN);
        
        List<Message> messages = chatHistoryService.getHistory(conversationId, lastN);
        int totalCount = chatHistoryService.getMessageCount(conversationId);
        long remainingTtl = chatHistoryService.getRemainingTtl(conversationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("totalCount", totalCount);
        result.put("returnedCount", messages.size());
        result.put("remainingTtl", remainingTtl);
        result.put("messages", messages);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 清除会话历史
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Map<String, Object>> clearHistory(@PathVariable String conversationId) {
        log.info("清除会话历史: conversationId={}", conversationId);
        
        chatHistoryService.clearHistory(conversationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "会话历史已清除");
        result.put("conversationId", conversationId);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取会话信息
     * @param conversationId 会话ID
     * @return 会话信息
     */
    @GetMapping("/{conversationId}/info")
    public ResponseEntity<Map<String, Object>> getConversationInfo(@PathVariable String conversationId) {
        log.info("查询会话信息: conversationId={}", conversationId);
        
        boolean exists = chatHistoryService.exists(conversationId);
        int messageCount = chatHistoryService.getMessageCount(conversationId);
        long remainingTtl = chatHistoryService.getRemainingTtl(conversationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("exists", exists);
        result.put("messageCount", messageCount);
        result.put("remainingTtl", remainingTtl);
        result.put("remainingTtlHours", remainingTtl > 0 ? remainingTtl / 3600.0 : 0);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 刷新会话过期时间
     * @param conversationId 会话ID
     * @return 操作结果
     */
    @PostMapping("/{conversationId}/refresh")
    public ResponseEntity<Map<String, Object>> refreshTtl(@PathVariable String conversationId) {
        log.info("刷新会话过期时间: conversationId={}", conversationId);
        
        chatHistoryService.refreshTtl(conversationId);
        long remainingTtl = chatHistoryService.getRemainingTtl(conversationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "会话过期时间已刷新");
        result.put("conversationId", conversationId);
        result.put("remainingTtl", remainingTtl);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 批量检查会话是否存在
     * @param conversationIds 会话ID列表
     * @return 各会话的存在状态
     */
    @PostMapping("/batch/check")
    public ResponseEntity<Map<String, Object>> batchCheckExists(
            @RequestBody List<String> conversationIds) {
        log.info("批量检查会话存在性: {}", conversationIds);
        
        Map<String, Boolean> existsMap = new HashMap<>();
        for (String conversationId : conversationIds) {
            existsMap.put(conversationId, chatHistoryService.exists(conversationId));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", conversationIds.size());
        result.put("exists", existsMap);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 分页查询会话历史
     * @param conversationId 会话ID
     * @param page 页码（从1开始，默认1）
     * @param size 每页大小（默认10）
     * @return 分页结果
     */
    @GetMapping("/{conversationId}/page")
    public ResponseEntity<Map<String, Object>> getHistoryByPage(
            @PathVariable String conversationId,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        
        log.info("分页查询会话历史: conversationId={}, page={}, size={}", conversationId, page, size);
        
        // 参数验证
        if (page < 1) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "页码不能小于1");
            return ResponseEntity.badRequest().body(error);
        }
        
        if (size <= 0 || size > 100) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "每页大小必须在1-100之间");
            return ResponseEntity.badRequest().body(error);
        }
        
        // 转换为从0开始的页码（内部使用）
        int internalPage = page - 1;
        ChatHistoryService.PageResult pageResult = chatHistoryService.getMessagesByPage(conversationId, internalPage, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("page", page);  // 返回用户传入的页码（从1开始）
        result.put("size", pageResult.getSize());
        result.put("totalMessages", pageResult.getTotalMessages());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("hasNext", pageResult.isHasNext());
        result.put("hasPrevious", pageResult.isHasPrevious());
        result.put("messages", pageResult.getMessages());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有会话ID列表（管理员接口）
     * @param adminKey 管理员密钥
     * @return 所有会话ID列表
     */
    @GetMapping("/admin/conversations")
    public ResponseEntity<Map<String, Object>> getAllConversations(
            @RequestParam(required = false) String adminKey) {
        
        log.info("管理员查询所有会话列表");
        
        // 验证管理员权限
        if (adminKey == null || !adminKey.equals(this.adminKey)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "无权限访问");
            error.put("message", "需要管理员权限才能访问此接口");
            log.warn("管理员接口访问被拒绝，提供的密钥: {}", adminKey != null ? "***" : "null");
            return ResponseEntity.status(403).body(error);
        }
        
        List<String> conversationIds = chatHistoryService.getAllConversationIds();
        
        // 获取每个会话的详细信息
        List<Map<String, Object>> conversationDetails = new ArrayList<>();
        for (String conversationId : conversationIds) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("conversationId", conversationId);
            detail.put("messageCount", chatHistoryService.getMessageCount(conversationId));
            detail.put("remainingTtl", chatHistoryService.getRemainingTtl(conversationId));
            conversationDetails.add(detail);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", conversationIds.size());
        result.put("conversations", conversationDetails);
        
        return ResponseEntity.ok(result);
    }
}

