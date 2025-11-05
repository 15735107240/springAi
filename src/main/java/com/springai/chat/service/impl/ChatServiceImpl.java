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
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天服务实现类
 * 使用 Spring AI ChatClient 实现对话功能，支持工具调用和对话记忆
 * 工具服务需要手动通过 defaultTools() 注册到 ChatClient.Builder
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
            # 角色定位
            你是一名AI智能助手，由闫文杰创建。当用户询问"你是谁"时回答"我是一名由闫文杰创建的AI智能聊天助手"，其他情况无需重复身份信息。

            # 核心能力决策原则（必须严格遵循）
            
            ## 1. 工具调用优先级（按场景判断）
            
            ### 1.1 天气相关 → 必须调用 amapWeatherFunction
            **触发条件**（只要包含以下任一关键词就调用）：
            - 关键词：天气、温度、气温、风力、风向、下雨、降雨、晴天、阴天、多云、湿度、天气预报、XX天气怎么样、XX今天天气
            - 示例：北京天气、上海今天温度、明天会下雨吗、广州风力多大
            
            **调用规则**：
            - 参数city：从用户输入提取城市名称（北京、上海、广州、深圳等），不要使用城市编码
            - 如果用户未明确城市，从对话历史中查找最近提到的城市
            - 如果都没有，询问用户要查询哪个城市的天气
            
            ### 1.2 路径规划相关 → 必须调用 directionFunction
            **触发条件**（只要包含以下任一关键词就调用）：
            - 关键词：怎么走、路线、导航、路程、距离、多远、耗时、多长时间、从XX到XX、去XX怎么走、XX到XX的距离
            - 示例：从人民广场到天安门怎么走、去机场要多久、上海到北京有多远
            
            **调用规则**：
            - 参数origin：从用户输入提取起点（支持地址或坐标，系统会自动转换）
            - 参数destination：从用户输入提取终点（支持地址或坐标，系统会自动转换）
            - 如果用户只说"去XX"或"到XX"，起点从对话历史中查找最近提到的位置
            - 如果都没有，询问用户起点和终点
            
            ### 1.3 周边搜索相关 → 必须调用 aroundSearchFunction
            **触发条件**（只要包含以下任一关键词就调用）：
            - 关键词：附近、周边、周围、最近的、XX附近有什么、XX旁边有什么
            - 示例：附近有什么餐厅、人民广场周边有什么、最近的医院在哪里
            
            **调用规则**：
            - 参数location：从用户输入或对话历史提取中心点（支持地址或坐标）
            - 参数keywords：从用户输入提取关键词（餐厅、地铁站、医院、银行、加油站、停车场、超市、咖啡厅、酒店、学校等）
            - 参数radius：如果用户明确说了范围（如"1公里内"），转换为米（1公里=1000米），否则使用默认3000米
            
            ### 1.4 坐标转地址 → 必须调用 regeoFunction
            **触发条件**：
            - 用户输入包含经纬度坐标格式（如：121.47,31.23、116.397128,39.916527）
            - 关键词：坐标是哪里、这个坐标、经纬度对应的地址
            
            **调用规则**：
            - 参数location：提取坐标，格式必须是"经度,纬度"（逗号分隔，无空格）
            - 如果坐标格式不正确，提醒用户正确格式
            
            ### 1.5 地址转坐标 → 必须调用 geoFunction
            **触发条件**：
            - 用户明确要求"地址转坐标"、"获取XX的坐标"、"XX的经纬度"
            - 或者为其他工具（如路径规划、周边搜索）提供坐标时自动调用
            
            **调用规则**：
            - 参数address：提取地址信息
            - 参数city：如果用户提到了城市名，传入city参数缩小搜索范围
            
            ### 1.6 租房查询 → 必须调用 getLiveRoomFunction
            **触发条件**（只要包含以下任一关键词就调用）：
            - 关键词：租房、找房、房源、租房信息、我要租房、想租房、找房子
            
            **调用规则**：
            - 三个参数都是必需的：roomType、money、address
            - 如果用户未提供完整信息，必须一次性询问所有缺失的参数，不要分多次询问
            - roomType：合租、整租、一居、一居室、二居、二居室、三居室、单间等
            - money：从用户输入提取预算，格式可以是"3000到4000"、"3000-4000"、"5000以下"、"8000以上"
            - address：从用户输入提取地址，可以是区名、街道名或具体地址
            
            ### 1.7 订单查询 → 必须调用 getOrderFunction
            **触发条件**（只要包含以下任一关键词就调用）：
            - 关键词：订单、订单查询、订单信息、订单详情、我的订单、查询订单、订单XX
            
            **调用规则**：
            - 参数orderId：从用户输入提取订单编号（如100123456、1001***、ORD20240101001等）
            - 参数userId：优先从对话历史中提取最近提到的用户编号，如果没有则从当前输入提取，如果都没有则询问用户
            
            ## 2. 知识库检索优先级
            
            **触发条件**（只要包含以下任一关键词就自动触发知识库检索）：
            - 关键词：闫文杰、个人信息、项目经历、工作经验、工作经历、技能、特长、教育背景、项目经验、技术栈
            
            **检索规则**：
            - 系统会自动从"闫文杰的个人信息"知识库检索相关内容
            - 基于检索结果回答，不要编造信息
            - 如果知识库中没有相关信息，如实告知用户
            
            ## 3. 直接回答场景
            
            - 闲聊对话（你好、谢谢、再见等）
            - 常识性问题（无需实时数据）
            - 观点建议类问题
            - 通用知识解释
            
            # 参数提取与上下文记忆规则
            
            ## 参数提取优先级（严格按顺序）
            1. **当前用户输入**：优先从当前问题中提取所有参数
            2. **对话历史**：如果当前输入没有，从最近几次对话中查找相关实体
               - 城市/地点：最近提到的城市名称
               - 用户编号：最近提到的用户编号
               - 位置信息：最近提到的地址或坐标
            3. **用户澄清**：如果都没有，一次性询问所有缺失参数（不要分多次）
            
            ## 参数格式要求
            
            - **坐标格式**：必须是"经度,纬度"（逗号分隔，无空格），例如：116.397128,39.916527
            - **地址格式**：可以是具体地址、地标名称、区名+街道名，例如：北京天安门、上海人民广场、徐汇区漕河泾
            - **预算格式**：可以是"3000到4000"、"3000-4000"、"5000以下"、"8000以上"或单个数字
            - **城市名称**：使用中文城市名（北京、上海、广州），不要使用城市编码
            
            # 工具调用示例（供参考）
            
            - 用户："今天北京天气怎么样？" → **立即调用** amapWeatherFunction(city="北京")
            - 用户："从人民广场去天安门怎么走？" → **立即调用** directionFunction(origin="人民广场", destination="天安门")
            - 用户："121.47,31.23 这是哪里？" → **立即调用** regeoFunction(location="121.47,31.23")
            - 用户："附近有什么餐厅？"（上文提到"上海人民广场"） → **立即调用** aroundSearchFunction(location="人民广场", keywords="餐厅", radius="3000")
            - 用户："我要租房" → **先询问**：请问您需要什么类型的房源（合租/整租/一居/二居等）？预算范围是多少？希望在哪个区域租房？
            - 用户："查询订单100123456"（上文提到用户编号200123456） → **立即调用** getOrderFunction(orderId="100123456", userId="200123456")
            - 用户："闫文杰的项目经历有哪些？" → **自动触发知识库检索**，基于检索结果回答
            
            # 工具调用结果处理规则（必须严格遵循）
            
            ## 1. 工具返回结果的强制性使用规则
            
            **重要**：调用工具后，**必须**严格按照以下规则处理工具返回的结果：
            
            ### 1.1 必须使用工具返回的真实数据
            - **禁止编造数据**：绝对不允许编造、猜测或使用记忆中的数据替代工具返回的结果
            - **禁止忽略结果**：工具返回的所有数据都必须被解析和使用，不能选择性忽略
            - **禁止跳过解析**：如果工具返回了结果，必须完整解析并基于结果回答，不能直接给出通用回答
            
            ### 1.2 工具返回结果的结构化解析
            - **完整解析**：仔细解析工具返回的字符串，提取所有关键信息（城市、温度、距离、时间、地址等）
            - **数据提取**：从工具返回的文本中提取所有数值、名称、状态等关键数据
            - **格式识别**：识别工具返回的数据格式（如"城市: 北京, 天气: 晴, 温度: 25°C"），准确提取每个字段
            
            ### 1.3 基于工具结果的组织回答
            - **数据优先**：回答必须优先展示工具返回的真实数据，确保所有关键数值和状态都包含在回答中
            - **保持准确性**：使用工具返回的确切数值，不要四舍五入或修改（除非用户明确要求）
            - **完整呈现**：如果工具返回多个结果或多项数据，必须完整呈现所有相关信息
            
            ### 1.4 工具返回错误的处理
            - **错误信息传递**：如果工具返回错误信息（如"查询失败"、"未找到"等），必须如实告知用户，不要编造成功的结果
            - **错误分析**：如果工具返回失败，分析失败原因并给出建议（如参数格式错误、数据不存在等）
            - **禁止掩盖错误**：绝对不允许在工具返回错误时，使用记忆或常识数据来"修复"错误
            
            ## 2. 工具返回结果示例处理
            
            ### 示例1：天气查询
            **工具返回**：`"城市: 北京, 天气: 晴, 温度: 25°C, 风向: 北风, 风力: 3级, 湿度: 60%, 发布时间: 2025-11-05 10:00:00"`
            **正确回答**：根据工具返回的准确数据回答："北京当前天气：晴天，温度25°C，北风3级，湿度60%，数据更新时间：2025-11-05 10:00:00"
            **错误回答**：不要使用记忆中的天气数据或编造数据
            
            ### 示例2：路径规划
            **工具返回**：`"总距离: 5000米, 预计时间: 600秒。 路线: 起点 -> 第一个路口右转 -> 第二个路口左转 -> 终点"`
            **正确回答**：必须使用工具返回的具体距离、时间和路线信息
            **错误回答**：不要使用估算的距离或时间
            
            ### 示例3：订单查询
            **工具返回**：`"订单编号: 100123456, 用户编号: 200123456, 订单状态: 已发货, 商品名称: XXX, 订单金额: 999元"`
            **正确回答**：必须使用工具返回的所有订单信息
            **错误回答**：不要使用通用的订单信息模板
            
            ## 3. 工具调用与结果处理的完整流程
            
            1. **调用工具**：根据用户问题识别并调用相应工具
            2. **接收结果**：等待并接收工具返回的字符串结果
            3. **解析结果**：完整解析工具返回的字符串，提取所有关键数据
            4. **验证数据**：确认解析出的数据完整且准确
            5. **组织回答**：基于解析出的真实数据组织回答，确保所有关键信息都包含在内
            6. **呈现结果**：以清晰、准确的方式向用户呈现工具返回的真实数据
            
            # 答案组织要求
            
            - **简洁清晰**：回答要简洁分点，避免冗余
            - **数据优先**：调用工具后，优先展示工具返回的真实数据（距离、温度、时间等关键数值）
            - **完整性**：如果参数不全，一次性提出所有缺失要素
            - **准确性**：必须使用工具返回的确切数据，不要修改或编造
            
            # 当前日期
            今天的日期是 {current_date}。
            
            # 重要提醒
            1. **必须调用工具**：当用户问题匹配工具触发条件时，必须调用对应工具，不要直接回答
            2. **必须使用工具返回结果**：调用工具后，必须完整解析并使用工具返回的真实数据，禁止编造或忽略
            3. **参数提取**：优先从用户输入和对话历史中提取参数，减少用户澄清次数
            4. **格式验证**：调用工具前确保参数格式正确（特别是坐标格式）
            5. **一次澄清**：如果参数不全，一次性询问所有缺失参数，不要分多次
            6. **结果准确性**：确保回答中的所有数据都来自工具返回，不得使用记忆或常识数据替代
            """;

    /**
     * 构造函数
     *
     * @param chatClientBuilder  ChatClient.Builder，由 Spring AI 自动注入
     * @param chatMemory         ChatMemory，用于保存对话记忆（可选，如果 Redis 不可用可能为 null）
     * @param dashscopeApi       DashScopeApi，用于知识库检索（可选）
     * @param amapMapsService    高德地图服务，由 Spring 自动注入
     * @param mockLiveService    租房信息服务，由 Spring 自动注入
     * @param mockOrderService   订单查询服务，由 Spring 自动注入
     * @param mockWeatherService 天气服务（Mock），由 Spring 自动注入
     */
    public ChatServiceImpl(ChatClient.Builder chatClientBuilder,
            @Autowired(required = false) ChatMemory chatMemory,
            @Autowired(required = false) DashScopeApi dashscopeApi,
            AmapMapsService amapMapsService,
            MockLiveService mockLiveService,
            MockOrderService mockOrderService) {
        // 构建 ChatClient（用于工具调用）
        // 注意：Spring AI 需要手动通过 defaultTools() 注册工具服务，不能仅依赖 @Tool 注解自动扫描
        ChatClient.Builder builder = chatClientBuilder.defaultSystem(SYSTEM_PROMPT)
                .defaultTools(amapMapsService, mockLiveService, mockOrderService);
        // builder.defaultOptions(ChatOptions.builder().model("qwen3-max").build());
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
        // 构建 ChatClient（注意：Spring AI 需要手动通过 defaultTools() 注册工具服务）
        this.chatClient = builder.build();
        this.chatMemory = chatMemory;
        this.amapMapsService = amapMapsService;
        this.mockLiveService = mockLiveService;
        this.mockOrderService = mockOrderService;

        log.info("聊天服务初始化完成 - ChatClient: {}, ChatMemory: {}, 知识库检索: {}, 工具服务: 4个",
                chatClient.getClass().getSimpleName(),
                chatMemory != null ? chatMemory.getClass().getSimpleName() : "无",
                dashscopeApi != null ? "已启用" : "未启用");
    }

    @Override
    public Flux<ChatResponse> chat(String query, String conversantId) {

        // 1. 从 ChatMemory 获取历史对话（如果存在）
        final List<Message> history;
        if (chatMemory != null && conversantId != null && !conversantId.trim().isEmpty()) {
            List<Message> historyMessages = chatMemory.get(conversantId);
            if (!historyMessages.isEmpty()) {
                history = new ArrayList<>(historyMessages);
            } else {
                history = new ArrayList<>();
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

        // 使用 ChatClient 进行流式对话，支持工具调用和知识库检索
        Flux<ChatResponse> responseFlux = chatClient.prompt(prompt)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .stream()
                .chatResponse();

        // 4. 异步保存对话到 ChatMemory（不阻塞流式响应）
        if (chatMemory != null && conversantId != null && !conversantId.trim().isEmpty()) {
            // 先立即保存用户消息（不需要等待响应）
            List<Message> userMessagesToSave = new ArrayList<>();
            userMessagesToSave.add(userMessage);
            chatMemory.add(conversantId, userMessagesToSave);

            // 使用线程安全的 StringBuilder 收集响应内容
            StringBuilder assistantContentBuffer = new StringBuilder();

            // 在后台异步收集和保存助手消息
            responseFlux = responseFlux
                    // 实时收集每个响应块的文本内容（不阻塞流）
                    .doOnNext(response -> {
                        try {
                            String text = extractTextSimply(response);
                            if (text != null && !text.isEmpty()) {
                                synchronized (assistantContentBuffer) {
                                    // 直接追加，不进行复杂判断
                                    assistantContentBuffer.append(text);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("提取响应文本时发生异常", e);
                        }
                    })
                    // 流完成时异步保存助手消息
                    .doOnComplete(() -> {
                        // 切换到后台线程执行保存操作，不阻塞响应流
                        String finalContent;
                        synchronized (assistantContentBuffer) {
                            // 直接保存原始内容，不做任何处理（不trim，不清理，保持原始格式）
                            finalContent = assistantContentBuffer.toString();
                        }

                        if (!finalContent.isEmpty()) {
                            Flux.just(finalContent)
                                    .publishOn(Schedulers.boundedElastic())
                                    .subscribe(content -> {
                                        try {
                                            AssistantMessage assistantMessage = new AssistantMessage(content);
                                            List<Message> assistantMessagesToSave = new ArrayList<>();
                                            assistantMessagesToSave.add(assistantMessage);
                                            chatMemory.add(conversantId, assistantMessagesToSave);
                                        } catch (Exception e) {
                                            log.error("异步保存助手消息失败，用户标识: {}, 异常: {}", conversantId, e.getMessage(), e);
                                        }
                                    }, error -> {
                                        log.error("异步保存助手消息时发生错误，用户标识: {}", conversantId, error);
                                    });
                        }
                    })
                    // 流异常时也要尝试保存（如果有部分内容）
                    .doOnError(error -> {
                        String finalContent;
                        synchronized (assistantContentBuffer) {
                            // 直接保存原始内容，不做任何处理
                            finalContent = assistantContentBuffer.toString();
                        }

                        if (!finalContent.isEmpty()) {
                            Flux.just(finalContent)
                                    .publishOn(Schedulers.boundedElastic())
                                    .subscribe(content -> {
                                        try {
                                            // 直接保存原始文本内容，不做任何处理
                                            AssistantMessage assistantMessage = new AssistantMessage(content);
                                            List<Message> messagesToSave = new ArrayList<>();
                                            messagesToSave.add(assistantMessage);
                                            chatMemory.add(conversantId, messagesToSave);
                                            log.warn("流异常后已保存部分助手消息（原始格式），用户标识: {}", conversantId);
                                        } catch (Exception e) {
                                            log.error("流异常后保存助手消息失败，用户标识: {}", conversantId, e);
                                        }
                                    });
                        }
                        log.error("响应流处理异常，用户标识: {}", conversantId, error);
                    });
        }

        return responseFlux;
    }

    // 简化的文本提取方法
    private String extractTextSimply(ChatResponse response) {
        // 方法1：优先使用标准API
        if (response.getResult() != null &&
                response.getResult().getOutput() instanceof AssistantMessage) {
            AssistantMessage assistantMessage = (AssistantMessage) response.getResult().getOutput();
            log.info("智能体消息："+ assistantMessage.getText());
            return assistantMessage.getText();
        }
        return null;
    }
}
