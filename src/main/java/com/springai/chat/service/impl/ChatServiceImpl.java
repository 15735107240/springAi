package com.springai.chat.service.impl;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.springai.chat.service.ChatService;
import com.springai.chat.tools.AmapMapsService;
import com.springai.chat.tools.MockLiveService;
import com.springai.chat.tools.MockOrderService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天服务实现类
 * 使用 Spring AI ChatClient 实现对话功能，支持工具调用和对话记忆
 * Spring AI 会自动扫描带有 @Tool 注解的工具方法
 * 
 * @author yanwenjie
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final AmapMapsService amapMapsService;
    private final MockLiveService mockLiveService;
    private final MockOrderService mockOrderService;

    private static final String SYSTEM_PROMPT = """
            你的身份与语气：
            - 当且仅当用户询问"你是谁"时回答"我是一名由闫文杰创建的AI智能聊天助手"。
            - 其他情况下无需重复身份信息，回答简洁清晰。

            ## 决策总则（基于对话上下文选择最合适能力）
            1. **工具优先**：当问题需要实时信息、外部系统数据或可执行操作时，优先调用相应工具。
               - 实时数据：天气、地址/坐标转换、导航路线、周边搜索
               - 业务操作：订单查询、租房信息查询
               - 时间相关：当前日期查询（系统已注入{current_date}）
            
            2. **知识库优先**：当问题涉及内部知识或事实密集型问答时，优先进行知识库检索。
               - 关于"闫文杰"的个人信息、项目经历、工作经验、技能特长
               - 公司制度、产品信息、技术文档等内部知识
               - 系统会自动从"闫文杰的个人信息"知识库中检索相关内容
            
            3. **大模型直答**：闲聊、观点建议、常识性解释、无需实时数据的泛化问题时直接回答。
            
            4. **不确定时**：默认先走知识库检索，再补充大模型组织答案。

            ## 上下文与实体记忆
            - **自动提取**：从对话历史中抽取缺省实体（最近一次提到的城市/地点/经纬度/人名/订单号/用户编号等）。
            - **智能补全**：若参数缺失且上下文可补全，则自动补全；若无法补全，先向用户澄清，再调用工具。
            - **一致性保持**：同一会话中优先沿用最近确认过的实体与单位（温度单位、地点城市、用户编号等）。

            ## 工具选择详细规则

            ### 高德地图工具
            **geoFunction（地理编码）**
            - 使用场景：用户提供地址需要获取坐标；为路径规划、周边搜索等操作提供位置坐标
            - 触发关键词："地址转坐标"、"获取XX的坐标"、"XX的经纬度"
            - 参数说明：address（必需，地址字符串），city（可选，城市名称，用于缩小搜索范围）
            - 返回结果：可能多个，优先使用第一个匹配结果

            **regeoFunction（逆地理编码）**
            - 使用场景：用户提供经纬度坐标需要知道具体位置
            - 触发关键词："坐标转地址"、"XX坐标是哪里"、"经纬度XX对应的地址"
            - 参数说明：location（必需，格式：经度,纬度，例如：116.397128,39.916527）
            - 注意：输入必须是"经度,纬度"格式，逗号分隔无空格

            **directionFunction（路径规划）**
            - 使用场景：用户询问路线、导航、路程、距离、耗时等问题
            - 触发关键词："怎么走"、"路线"、"导航"、"路程"、"距离"、"多远"、"耗时"、"多长时间"、"从XX到XX"
            - 参数说明：origin（起点，支持地址或坐标），destination（终点，支持地址或坐标）
            - 自动处理：系统会自动将地址转换为坐标，无需手动转换
            - 返回信息：总距离（米）、预计时间（秒）、关键路线步骤

            **aroundSearchFunction（周边搜索）**
            - 使用场景：用户询问附近、周边、周围的兴趣点
            - 触发关键词："附近有什么"、"周边XX"、"周围XX"、"最近的XX"、"XX附近"
            - 参数说明：
              - location（中心点，支持地址或坐标，系统自动转换）
              - keywords（搜索关键词，如：餐厅、地铁站、医院、银行、加油站、停车场、超市、咖啡厅）
              - radius（搜索半径，单位：米，可选，默认3000米，建议范围500-5000米）
            - 返回结果：最多返回10个结果，包含名称、地址、距离

            **amapWeatherFunction（天气查询）**
            - 使用场景：用户询问天气、温度、风力、是否下雨、天气预报等
            - 触发关键词："天气"、"温度"、"风力"、"下雨"、"天气预报"、"XX天气怎么样"
            - 参数说明：city（城市名称或城市编码adcode，优先使用城市名称，如：北京、上海、广州）
            - 返回信息：城市、天气状况、温度、风向、风力、湿度、发布时间

            ### 业务工具
            **getLiveRoomFunction（租房信息查询）**
            - 使用场景：用户询问租房、找房、房源、租房信息等
            - 触发关键词："租房"、"找房"、"房源"、"租房信息"、"我要租房"
            - 参数说明：
              - roomType（租房类型：合租、整租、一居、一居室、二居、二居室、三居室、单间等）
              - money（预算范围：格式可以是"3000到4000"、"3000-4000"、"5000以下"等）
              - address（租房地址：区名、街道名或具体地址，如：徐汇区漕河泾、北京朝阳区）
            - 注意：三个参数都是必需的，如果用户未提供完整信息，应一次性询问所有缺失的参数

            **getOrderFunction（订单查询）**
            - 使用场景：用户询问订单、订单查询、订单信息、订单详情等
            - 触发关键词："我的订单"、"订单查询"、"订单信息"、"订单详情"、"订单XX"
            - 参数说明：
              - orderId（订单编号：如100123456、1001***、ORD20240101001等，从用户输入提取）
              - userId（用户编号：如200123456、2001***、USER001等，可从对话历史中提取）
            - 注意：可以从对话历史中提取之前提到过的用户编号

            ## 工具调用参数策略（重要）
            1. **地址转坐标**：若工具需要经纬度但用户给出地址，系统会自动调用geoFunction转换，无需手动转换
            2. **坐标转地址**：用户提供坐标需要地址时，直接调用regeoFunction
            3. **参数提取优先级**：
               - 优先从当前用户输入中提取
               - 其次从对话历史中提取最近提到的实体
               - 最后向用户澄清获取
            4. **参数格式验证**：
               - 坐标格式：必须是"经度,纬度"（逗号分隔，无空格）
               - 地址格式：可以是具体地址、地标名称、区名+街道名
               - 数字范围：预算范围可以是"X到Y"、"X-Y"或单个数字

            ## 知识库使用策略
            - 当用户问题与"闫文杰"、"个人信息"、"项目经历"、"工作经验"、"技能"等相关时，系统会自动从"闫文杰的个人信息"知识库检索
            - 检索结果会自动注入到上下文中，基于检索结果回答，不要编造信息
            - 如果知识库中未找到相关信息，如实告知用户

            ## 答案风格与完整性
            - **简洁清晰**：回答要简洁分点，高信噪比，避免冗余信息
            - **关键数值**：必要时要给出关键数值（距离、时长、温度、风力、湿度等）
            - **工具结果优先**：若调用了工具，优先反馈工具返回的"最新与真实"数据
            - **多结果处理**：对多个候选结果，列出前几项并说明选择依据
            - **参数澄清**：若参数不全，一次性提出所有缺失要素，不要分多次询问

            ## 典型使用示例（供决策参考，非输出内容）
            - 用户："今天北京天气怎么样？" → 调用 amapWeatherFunction(city="北京")
            - 用户："从人民广场去天安门怎么走？" → 调用 directionFunction(origin="人民广场", destination="天安门")
            - 用户："121.47,31.23 这是哪里？" → 调用 regeoFunction(location="121.47,31.23")
            - 用户："附近有什么餐厅？"（上文提到"上海人民广场"） → 调用 aroundSearchFunction(location="人民广场", keywords="餐厅", radius="3000")
            - 用户："我要租房" → 询问缺失参数（类型、预算、地址），然后调用 getLiveRoomFunction
            - 用户："查询订单100123456" → 从历史提取用户编号，调用 getOrderFunction(orderId="100123456", userId="从历史提取")
            - 用户："闫文杰的项目经历有哪些？" → 自动触发知识库检索，基于检索结果回答

            今天的日期是 {current_date}。
            """;

    /**
     * 构造函数
     * 
     * @param chatClientBuilder ChatClient.Builder，由 Spring AI 自动注入
     * @param chatMemory        ChatMemory，用于保存对话记忆（可选，如果 Redis 不可用可能为 null）
     * @param dashscopeApi      DashScopeApi，用于知识库检索（可选）
     * @param amapMapsService   高德地图服务，由 Spring 自动注入
     * @param mockLiveService   租房信息服务，由 Spring 自动注入
     * @param mockOrderService  订单查询服务，由 Spring 自动注入
     */
    public ChatServiceImpl(ChatClient.Builder chatClientBuilder,
            @Autowired(required = false) ChatMemory chatMemory,
            @Autowired(required = false) DashScopeApi dashscopeApi,
            AmapMapsService amapMapsService,
            MockLiveService mockLiveService,
            MockOrderService mockOrderService) {
        // 构建 ChatClient（用于工具调用）
        ChatClient.Builder builder = chatClientBuilder.defaultSystem(SYSTEM_PROMPT)
                .defaultTools(amapMapsService, mockLiveService, mockOrderService);

        // 如果 DashScopeApi 存在，添加知识库检索 Advisor
        if (dashscopeApi != null) {
            try {
                DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeApi,
                        DashScopeDocumentRetrieverOptions.builder().withIndexName("闫文杰的个人信息").build());
                builder.defaultAdvisors(new DocumentRetrievalAdvisor(retriever));
                log.info("知识库检索 Advisor 配置成功 - 索引名称: 闫文杰的个人信息");
            } catch (Exception e) {
                log.error("配置知识库检索 Advisor 失败，知识库检索功能将不可用", e);
            }
        } else {
            log.warn("DashScopeApi 未配置，知识库检索功能将不可用。如需启用，请确保 DashScope API Key 配置正确。");
        }
        // Spring AI 会自动扫描 @Tool 注解的方法，无需手动注册
        this.chatClient = builder.build();
        this.chatMemory = chatMemory;
        this.amapMapsService = amapMapsService;
        this.mockLiveService = mockLiveService;
        this.mockOrderService = mockOrderService;
        log.info("聊天服务初始化完成 - ChatClient: {}, ChatMemory: {}, 知识库检索: {}",
                chatClient.getClass().getSimpleName(),
                chatMemory != null ? chatMemory.getClass().getSimpleName() : "无",
                dashscopeApi != null ? "已启用" : "未启用");
    }

    @Override
    public Flux<ChatResponse> chat(String query, String conversantId) {
        log.info("收到聊天请求 - 消息: {}, 用户标识: {}", query, conversantId);

        // 1. 从 ChatMemory 获取历史对话（如果存在）
        final List<Message> history;
        if (chatMemory != null && conversantId != null && !conversantId.trim().isEmpty()) {
            List<Message> historyMessages = chatMemory.get(conversantId);
            if (historyMessages != null && !historyMessages.isEmpty()) {
                history = new ArrayList<>(historyMessages);
                log.info("从对话历史中加载了 {} 条消息，用户标识: {}",
                        history.size(), conversantId);
            } else {
                history = new ArrayList<>();
                log.debug("未找到对话历史，用户标识: {}", conversantId);
            }
        } else {
            history = new ArrayList<>();
        }

        // 2. 构建消息列表（包含历史消息和当前用户消息）
        List<Message> messages = new ArrayList<>();

        // 添加系统消息（如果历史消息中没有系统消息）
        boolean hasSystemMessage = history.stream()
                .anyMatch(SystemMessage.class::isInstance);
        if (!hasSystemMessage) {
            messages.add(new SystemMessage(SYSTEM_PROMPT));
        }

        // 添加历史消息
        messages.addAll(history);

        // 添加当前用户消息
        UserMessage userMessage = new UserMessage(query);
        messages.add(userMessage);

        // 3. 使用 ChatClient 进行流式对话，传递历史消息以支持多轮对话
        // ChatClient 包含了系统提示词、知识库检索 Advisor、工具调用等所有配置
        // 使用 Prompt 对象传递完整的消息列表（包含历史），这样可以保持多轮对话上下文
        Prompt prompt = new Prompt(messages);

        // 使用 ChatClient 而不是 ChatModel，这样可以获取所有配置（系统提示词、知识库检索、工具调用等）
        // 使用注入的工具服务实例，确保 @Value 配置能够正确注入
        Flux<ChatResponse> responseFlux = chatClient.prompt(prompt)
                .options(ChatOptions.builder().model("qwen3-max").build())
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .stream()
                .chatResponse();

        // 4. 保存对话到 ChatMemory（在响应流完成后）
        if (chatMemory != null && conversantId != null && !conversantId.trim().isEmpty()) {
            final int historySize = history.size(); // 保存历史大小供 lambda 使用
            responseFlux = responseFlux
                    .collectList()
                    .flatMapMany(responses -> {
                        // 保存用户消息和所有助手响应到 ChatMemory
                        List<Message> messagesToSave = new ArrayList<>();
                        messagesToSave.add(userMessage);

                        // 从所有响应中提取助手消息
                        StringBuilder assistantContent = new StringBuilder();
                        for (ChatResponse response : responses) {
                            if (response.getResult() != null) {
                                // 方法1: 尝试从 Result 中获取消息
                                var result = response.getResult();

                                // 尝试获取 Output（通常是 Generation 对象）
                                if (result.getOutput() != null) {
                                    String content = extractTextContent(result.getOutput());
                                    if (content != null && !content.trim().isEmpty()) {
                                        assistantContent.append(content);
                                    }
                                }

                                // 方法2: 尝试从 Result 直接获取消息
                                try {
                                    java.lang.reflect.Method getOutputMethod = result.getClass().getMethod("getOutput");
                                    Object output = getOutputMethod.invoke(result);
                                    if (output != null) {
                                        String content = extractTextContent(output);
                                        if (content != null && !content.trim().isEmpty()) {
                                            if (assistantContent.length() > 0 &&
                                                    !assistantContent.toString().contains(content)) {
                                                assistantContent.append(content);
                                            } else if (assistantContent.length() == 0) {
                                                assistantContent.append(content);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // 忽略反射异常
                                }
                            }
                        }

                        // 如果有助手回复，添加到消息列表（清理重复内容）
                        String finalContent = assistantContent.toString().trim();
                        if (!finalContent.isEmpty()) {
                            // 移除重复的 AssistantMessage 描述
                            finalContent = cleanAssistantContent(finalContent);
                            AssistantMessage assistantMessage = new AssistantMessage(finalContent);
                            messagesToSave.add(assistantMessage);
                        }

                        if (!messagesToSave.isEmpty()) {
                            chatMemory.add(conversantId, messagesToSave);
                            log.info("保存了 {} 条消息到对话历史，用户标识: {} (总计: {} 条)",
                                    messagesToSave.size(), conversantId,
                                    historySize + messagesToSave.size());
                        }

                        // 返回响应流
                        return Flux.fromIterable(responses);
                    });
        }

        return responseFlux;
    }

    /**
     * 从 Generation 对象中提取文本内容
     * 支持多种方式尝试提取实际文本
     * 
     * @param output Generation 对象
     * @return 提取的文本内容，如果无法提取则返回 null
     */
    private String extractTextContent(Object output) {
        if (output == null) {
            return null;
        }

        try {
            // 方法1: 尝试调用 getContent() 方法
            try {
                java.lang.reflect.Method getContentMethod = output.getClass().getMethod("getContent");
                Object contentObj = getContentMethod.invoke(output);
                if (contentObj != null) {
                    String content = contentObj.toString();
                    if (content != null && !content.trim().isEmpty()
                            && !content.startsWith("org.springframework")) {
                        return content;
                    }
                }
            } catch (NoSuchMethodException e) {
                // getContent() 方法不存在，继续尝试其他方法
            }

            // 方法2: 尝试调用 getText() 方法
            try {
                java.lang.reflect.Method getTextMethod = output.getClass().getMethod("getText");
                Object textObj = getTextMethod.invoke(output);
                if (textObj != null) {
                    String text = textObj.toString();
                    if (text != null && !text.trim().isEmpty()
                            && !text.startsWith("org.springframework")) {
                        return text;
                    }
                }
            } catch (NoSuchMethodException e) {
                // getText() 方法不存在，继续尝试其他方法
            }

            // 方法3: 尝试获取 textContent 字段
            try {
                java.lang.reflect.Field textContentField = output.getClass().getDeclaredField("textContent");
                textContentField.setAccessible(true);
                Object textContent = textContentField.get(output);
                if (textContent != null) {
                    String text = textContent.toString();
                    if (text != null && !text.trim().isEmpty()
                            && !text.startsWith("org.springframework")) {
                        return text;
                    }
                }
            } catch (NoSuchFieldException e) {
                // textContent 字段不存在
            }

            // 方法4: 尝试 toString()，但过滤掉对象描述
            String toString = output.toString();
            if (toString != null && !toString.trim().isEmpty()
                    && !toString.startsWith("org.springframework")
                    && !toString.contains("AssistantMessage [")
                    && !toString.contains("messageType=")) {
                // 如果 toString 看起来像实际内容（不是对象描述），返回它
                if (toString.length() < 500 && !toString.contains("metadata=")) {
                    return toString;
                }
            }

            // 方法5: 如果是 AssistantMessage 类型，尝试通过 toString 然后解析
            if (output instanceof org.springframework.ai.chat.messages.AssistantMessage) {
                // AssistantMessage 在流式响应中可能是分片的，尝试提取实际文本
                String msgToString = output.toString();
                // 尝试从 toString 中提取 textContent
                if (msgToString.contains("textContent=")) {
                    int startIdx = msgToString.indexOf("textContent=") + 12;
                    int endIdx = msgToString.indexOf(",", startIdx);
                    if (endIdx == -1)
                        endIdx = msgToString.indexOf("}", startIdx);
                    if (endIdx > startIdx) {
                        String extracted = msgToString.substring(startIdx, endIdx);
                        if (!extracted.trim().isEmpty() && !extracted.startsWith("org.springframework")) {
                            return extracted;
                        }
                    }
                }
            }

            log.warn("无法从输出对象中提取文本内容，类型: {}", output.getClass().getName());
            return null;
        } catch (Exception e) {
            log.error("提取文本内容时发生异常", e);
            return null;
        }
    }

    /**
     * 清理助手消息内容，从 AssistantMessage 对象描述中提取纯文本内容
     * 
     * 处理格式: "AssistantMessage [..., textContent=实际文本, ...]"
     * 提取所有 textContent 值并拼接
     * 
     * @param content 原始内容
     * @return 清理后的纯文本内容
     */
    private String cleanAssistantContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        // 如果内容包含 AssistantMessage 对象描述，提取所有 textContent
        if (content.contains("textContent=")) {
            StringBuilder cleaned = new StringBuilder();
            int lastIndex = 0;
            final String searchPattern = "textContent=";

            while (true) {
                int startIdx = content.indexOf(searchPattern, lastIndex);
                if (startIdx == -1)
                    break;

                startIdx += searchPattern.length();

                // 查找 textContent 值的结束位置（逗号、右括号或 metadata）
                int endIdx = content.indexOf(", metadata=", startIdx);
                if (endIdx == -1) {
                    endIdx = content.indexOf("}", startIdx);
                }
                if (endIdx == -1) {
                    endIdx = content.indexOf(",", startIdx);
                }
                if (endIdx == -1) {
                    // 如果都没找到，可能是最后一个，查找下一个 AssistantMessage 或字符串结束
                    int nextMsg = content.indexOf("AssistantMessage [", startIdx);
                    if (nextMsg != -1) {
                        endIdx = nextMsg;
                    } else {
                        endIdx = content.length();
                    }
                }

                if (endIdx > startIdx) {
                    String text = content.substring(startIdx, endIdx).trim();
                    // 移除可能的引号
                    if (text.startsWith("\"") && text.endsWith("\"")) {
                        text = text.substring(1, text.length() - 1);
                    }
                    if (!text.isEmpty()) {
                        cleaned.append(text);
                    }
                }

                lastIndex = endIdx;
            }

            if (cleaned.length() > 0) {
                return cleaned.toString();
            }
        }

        // 如果内容不包含对象描述，直接返回（已经是纯文本）
        return content;
    }
}
