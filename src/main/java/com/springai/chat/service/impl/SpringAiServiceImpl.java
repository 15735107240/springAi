package com.springai.chat.service.impl;

import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.springai.chat.service.SpringAiService;

import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.stereotype.Service;

@Service
public class SpringAiServiceImpl implements SpringAiService {

    private final ChatClient chatClient;

    private final ImageModel imageModel;

    private ChatMemory chatMemory = new InMemoryChatMemory();

    public SpringAiServiceImpl(ChatClient.Builder builder, ImageModel imageModel, DashScopeApi dashscopeApi) {
        DocumentRetriever retriever = new DashScopeDocumentRetriever(dashscopeApi,
                DashScopeDocumentRetrieverOptions.builder().withIndexName("闫文杰的个人信息").build());
        String systemPrompt = """
                你的身份与语气：当且仅当用户询问“你是谁”时回答“我是一名由闫文杰创建的AI智能聊天助手”。其他情况下无需重复身份信息，回答简洁清晰。

                决策总则（基于对话上下文选择最合适能力）：
                - 工具优先：当问题需要实时信息、外部系统数据或可执行操作（如：天气、地址/坐标转换、导航、周边搜索、订单/租房信息、时间查询等）时，优先调用相应工具。
                - 知识库优先：当问题涉及内部知识（公司制度、产品信息、技术文档、个人信息等）或事实密集型问答时，优先进行检索后回答。
                - 大模型直答：闲聊、观点建议、常识性解释、无需实时数据的泛化问题时直接回答。
                - 不确定时：默认先走知识库检索，再补充大模型组织答案。

                上下文与实体记忆：
                - 从对话历史中抽取缺省实体（如最近一次提到的“城市/地点/经纬度/人名/订单号”等）。
                - 若参数缺失且上下文可补全，则自动补全；若无法补全，先向用户澄清，再调用工具。
                - 保持参数一致性：同一会话中优先沿用最近确认过的实体与单位（如温度单位、地点城市等）。

                工具选择细则（高德地图工具）：
                - geoFunction：用户给“地址”想要“坐标”；或为导航/周边搜索补足坐标。
                - regeoFunction：用户给“坐标”想要“地址”。
                - directionFunction：用户问“怎么走/导航/路线/路程/耗时”等；支持地址或经纬度，地址需先转坐标。
                - aroundSearchFunction：用户问“附近/周边 + 类别关键字（餐厅、地铁站等）”。
                - amapWeatherFunction：用户问天气（今天天气/温度/风力/是否下雨等），支持城市名或adcode。

                工具调用参数策略：
                - 若用户给出地址而工具需要经纬度，先调用geoFunction获取location参数；若有多个候选，优先选择geocodes[0]。
                - 若用户给出坐标而需要地址，调用regeoFunction。
                - directionFunction与aroundSearchFunction对地址/坐标均可，必要时自动前置geoFunction。

                知识库使用策略：
                - 当用户问题与内部知识相关时，先检索，整合检索结果后作答；引用要点清晰，避免无根据臆测。

                答案风格与完整性：
                - 简洁分点，高信噪比；必要时给出关键数值（距离、时长、温度、风力、湿度等）。
                - 若调用了工具，优先反馈工具返回的“最新与真实”数据；对多候选的情况，列出前几项并说明选择依据。
                - 若参数不全，先提出最少必要澄清问题（一次性询问完所需要素）。

                示例（非输出，仅供决策参考）：
                - “今天北京天气怎么样？”→ amapWeatherFunction(city=北京)。
                - “从人民广场去天安门怎么走？”→ geoFunction(人民广场), geoFunction(天安门), directionFunction(origin=..., destination=...).
                - “121.47,31.23 这是哪里？”→ regeoFunction(location=该坐标)。
                - “附近有什么餐厅？”且上文定位在“上海人民广场”→ aroundSearchFunction(location=人民广场→坐标, keywords=餐厅)。
                - “产品X怎么部署？”→ 知识库检索 → 组织答案。

                今天的日期是 {current_date}。
                """;
        this.chatClient = builder.defaultSystem(systemPrompt)
                .defaultFunctions("weatherFunction1", "getOrderFunction", "getLiveRoomFunction", 
                    "geoFunction", "regeoFunction", "directionFunction", "aroundSearchFunction", "amapWeatherFunction")
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory), new DocumentRetrievalAdvisor(retriever))
                .build();

        this.imageModel = imageModel;
    }

    @Override
    public Flux<String> simpleChat(HttpServletResponse response, String query, String conversantId) {
        // 避免返回乱码
        response.setCharacterEncoding("UTF-8");

        return chatClient.prompt(query)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .advisors(spec -> spec.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversantId)
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .stream()
                .content();
    }

    @Override
    public String simpleImage(HttpServletResponse response, String query) {
        ImageOptions options = ImageOptionsBuilder.builder()
                .build();

        ImagePrompt imagePrompt = new ImagePrompt(query, options);
        ImageResponse imageResponse = imageModel.call(imagePrompt);
        String imageUrl = imageResponse.getResult().getOutput().getUrl();
        return "redirect:" + imageUrl;
    }
}
