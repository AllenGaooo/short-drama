package com.bunny.flix.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spring AI 版对话服务
 *
 * 与规则版（AgentService.TOOL_KEYWORDS）的区别：
 * - 规则版：靠关键词匹配决定调哪个工具，零延迟零成本，但长尾表达覆盖不到
 * - AI 版：把工具清单交给大模型，由模型自主判断调哪个、传什么参数（function calling）
 *
 * 生产建议：两者混合——先走规则（快且省），规则没命中再交给模型（准且全）。
 * 这个混合调度在 AgentService 里完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpringAiAgentService {

    /** 系统提示：定义助手人设与工具使用边界 */
    private static final String SYSTEM_PROMPT =
            "你是兔兔短剧平台的智能助手，帮用户找剧、推荐短剧、解答平台问题。\n"
            + "规则：\n"
            + "1. 需要查询短剧数据时，调用可用的工具，不要凭空编造剧名或信息。\n"
            + "2. 工具返回的结果要整理成自然的推荐语，不要直接吐 JSON。\n"
            + "3. 如果用户表达含糊（如'随便看看'），可以调用推荐工具给几部热门剧。\n"
            + "4. 回答简洁，控制在 3~5 句话。";

    private final ChatClient chatClient;

    /**
     * 让大模型自主决定调用哪些工具并生成回复
     *
     * @param message 用户问题（已经过指代消解）
     * @param history 多轮历史
     * @return 模型最终回复
     */
    public String chat(String message, List<Map<String, String>> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        // 多轮历史（兼容 {role:文本} 与标准格式）
        if (history != null) {
            for (Map<String, String> h : history) {
                String content = h.get("content") != null ? h.get("content") : h.get("role");
                if (content == null || content.isBlank()
                        || "user".equals(content) || "assistant".equals(content)) {
                    continue;
                }
                messages.add(new UserMessage(content));
            }
        }
        messages.add(new UserMessage(message));

        Prompt prompt = new Prompt(messages, OpenAiChatOptions.builder()
                // 启用 function calling：模型自己决定是否调工具（0.8.1 接收 Set<String>）
                .withFunctions(Set.of("queryDrama", "recommendDrama", "getHotRanking", "getDramaDetail"))
                .build());

        var response = chatClient.call(prompt);
        if (response == null || response.getResult() == null) {
            return null;
        }
        AssistantMessage assistantMessage = response.getResult().getOutput();
        String content = assistantMessage != null ? assistantMessage.getContent() : null;

        log.info("[spring-ai] 模型回复生成成功，长度={}", content == null ? 0 : content.length());
        return content;
    }

    /** 判断 Spring AI 是否可用（未配 key 时不可用，走规则/本地兜底） */
    public boolean available() {
        return chatClient != null;
    }
}
