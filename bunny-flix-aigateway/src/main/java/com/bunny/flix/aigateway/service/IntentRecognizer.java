package com.bunny.flix.aigateway.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 意图识别
 * 对用户输入做轻量级分类，输出意图类型与置信度。对应架构图中的「意图识别」节点。
 *
 * 意图枚举：
 *   RAG    —— 需要检索平台知识/文档（是什么、介绍、规则、如何）
 *   GRAPH  —— 需要查业务关系/图谱（关系、关联、属于、谁、上下游）
 *   HYBRID —— 同时需要 RAG + 图谱
 *   LLM    —— 闲聊/生成类，直接走大模型
 */
@Component
public class IntentRecognizer {

    /** 图谱类意图关键词 */
    private static final String[] GRAPH_WORDS = {"关系", "关联", "图谱", "连接", "链路", "上下游", "属于", "谁", "哪些", "对应"};
    /** 知识检索类意图关键词 */
    private static final String[] RAG_WORDS = {"是什么", "介绍", "怎么", "如何", "规则", "说明", "平台", "会员", "优惠券", "知识", "含义"};

    public Map<String, Object> recognize(String query) {
        boolean graph = containsAny(query, GRAPH_WORDS);
        boolean rag = containsAny(query, RAG_WORDS);

        String intent;
        double confidence;
        if (graph && rag) {
            intent = "HYBRID";
            confidence = 0.95;
        } else if (graph) {
            intent = "GRAPH";
            confidence = 0.9;
        } else if (rag) {
            intent = "RAG";
            confidence = 0.9;
        } else {
            intent = "LLM";
            confidence = 0.7;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("intent", intent);
        out.put("confidence", confidence);
        out.put("hitGraph", graph);
        out.put("hitRag", rag);
        return out;
    }

    private boolean containsAny(String text, String[] words) {
        if (text == null) return false;
        for (String w : words) {
            if (text.contains(w)) return true;
        }
        return false;
    }
}
