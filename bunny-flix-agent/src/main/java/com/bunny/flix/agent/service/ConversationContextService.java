package com.bunny.flix.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多轮对话上下文服务（指代消解）
 *
 * 解决的问题：用户会用「上一部」「类似刚才那部」「这部」「换一部」这类
 * 依赖上下文的表达，直接拿这句话去检索是无法命中的——必须先把它
 * 还原成具体的短剧名称，再做检索/工具调用。
 *
 * 实现思路（轻量方案，无需大模型）：
 * 1. 每次工具调用返回短剧列表时，把「最近提及的短剧」记录到会话上下文
 * 2. 下一轮输入若包含指代词，用记录的剧名重写 query
 * 3. 上下文按用户维度存储，带过期时间，避免无限增长
 *
 * 生产环境可换成 Redis（支持多实例共享）或交给大模型做指代消解。
 */
@Slf4j
@Service
public class ConversationContextService {

    /** 指代词 → 替换模板，%s 为上一部剧名 */
    private static final Map<String, String> REFERENCE_WORDS = new LinkedHashMap<>();

    static {
        // 「上一部/上一集/刚才那部/那部/这部」→ 直接替换为剧名
        REFERENCE_WORDS.put("上一部", "%s");
        REFERENCE_WORDS.put("上一集", "%s");
        REFERENCE_WORDS.put("刚才那部", "%s");
        REFERENCE_WORDS.put("刚刚那部", "%s");
        REFERENCE_WORDS.put("那部", "%s");
        REFERENCE_WORDS.put("这部", "%s");
        REFERENCE_WORDS.put("这个", "%s");
        // 「类似的/这种的/再来一部/换一部」→ 基于上一部做相似推荐
        REFERENCE_WORDS.put("类似的", "%s 类似");
        REFERENCE_WORDS.put("相似的", "%s 类似");
        REFERENCE_WORDS.put("这种的", "%s 类似");
        REFERENCE_WORDS.put("再来一部", "%s 类似");
        REFERENCE_WORDS.put("换一部", "%s 类似");
        REFERENCE_WORDS.put("还有吗", "%s 类似");
    }

    /** 会话上下文：userId -> 上下文对象（内存实现，生产建议 Redis） */
    private final Map<Long, ConversationState> states = new ConcurrentHashMap<>();

    /** 上下文有效期：30 分钟无交互则清理 */
    private static final long STATE_TTL_MS = 30 * 60 * 1000L;

    /**
     * 指代消解：把含上下文指代的句子重写为可检索的明确语句
     *
     * @param userId  用户ID
     * @param message 原始输入
     * @return 重写后的语句（无指代时原样返回）
     */
    public String resolveReference(Long userId, String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        ConversationState state = states.get(userId);
        if (state == null || state.isExpired()) {
            states.remove(userId);
            return message;
        }

        String lastDrama = state.getLastDramaTitle();
        if (lastDrama == null || lastDrama.isBlank()) {
            return message;
        }

        for (Map.Entry<String, String> entry : REFERENCE_WORDS.entrySet()) {
            String word = entry.getKey();
            if (message.contains(word)) {
                String rewritten = message.replace(word, String.format(entry.getValue(), lastDrama));
                log.info("[agent] 指代消解: userId={} 原句={} 重写={} 上下文剧名={}",
                        userId, message, rewritten, lastDrama);
                state.touch();
                return rewritten;
            }
        }
        return message;
    }

    /**
     * 记录本轮提到的短剧（工具返回后调用）
     */
    public void recordDrama(Long userId, String dramaTitle) {
        if (userId == null || dramaTitle == null || dramaTitle.isBlank()) {
            return;
        }
        states.computeIfAbsent(userId, k -> new ConversationState()).setLastDramaTitle(dramaTitle);
        log.debug("[agent] 记录上下文剧名 userId={} drama={}", userId, dramaTitle);
    }

    /** 获取上一部剧名（供前端展示"正在围绕哪部剧对话"） */
    public String getLastDrama(Long userId) {
        ConversationState state = states.get(userId);
        if (state == null || state.isExpired()) {
            return null;
        }
        return state.getLastDramaTitle();
    }

    /** 清空某用户的上下文（如用户点击"新对话"） */
    public void clear(Long userId) {
        states.remove(userId);
    }

    /** 定期清理过期上下文（可由定时任务调用） */
    public void cleanExpired() {
        states.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    /** 会话状态 */
    private static class ConversationState {
        private String lastDramaTitle;
        private long lastAccessTime = System.currentTimeMillis();

        public String getLastDramaTitle() {
            return lastDramaTitle;
        }

        public void setLastDramaTitle(String lastDramaTitle) {
            this.lastDramaTitle = lastDramaTitle;
            this.lastAccessTime = System.currentTimeMillis();
        }

        public void touch() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastAccessTime > STATE_TTL_MS;
        }
    }
}
