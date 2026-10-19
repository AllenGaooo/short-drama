package com.bunny.flix.mcp.service;

import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具实现
 * 这里用内存示例数据模拟业务工具。真实环境下可替换为对 service-drama / service-user 等的 Feign 调用。
 */
@Service
public class McpToolService {

    /** 工具元信息描述（供 Agent 决定何时调用） */
    public List<Map<String, Object>> listTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(describe("query_drama", "根据关键词查询短剧列表",
                Map.of("keyword", "短剧名称关键字（可空，空则返回热门）")));
        tools.add(describe("query_user", "根据手机号或昵称查询用户信息",
                Map.of("keyword", "手机号或昵称关键字")));
        tools.add(describe("recommend_drama", "根据用户偏好或指定短剧推荐相似短剧",
                Map.of("keyword", "参考短剧名称或题材关键词（可空，空则返回热门推荐）")));
        tools.add(describe("get_drama_detail", "查询短剧详情（简介、集数、主演、题材）",
                Map.of("dramaId", "短剧ID")));
        tools.add(describe("get_hot_ranking", "获取热门短剧榜单",
                Map.of("size", "返回条数，默认10")));
        tools.add(describe("send_notice", "向指定用户发送站内通知",
                Map.of("userId", "用户ID", "content", "通知内容")));
        return tools;
    }

    /** 工具分发执行 */
    public Object call(String name, Map<String, Object> args) {
        if (name == null) {
            return Map.of("error", "tool name is required");
        }
        return switch (name) {
            case "query_drama" -> queryDrama(str(args, "keyword"));
            case "query_user" -> queryUser(str(args, "keyword"));
            case "recommend_drama" -> recommendDrama(str(args, "keyword"));
            case "get_drama_detail" -> getDramaDetail(str(args, "dramaId"));
            case "get_hot_ranking" -> getHotRanking(str(args, "size"));
            case "send_notice" -> sendNotice(str(args, "userId"), str(args, "content"));
            default -> Map.of("error", "unknown tool: " + name);
        };
    }

    /** 示例短剧库（真实环境应改为 Feign 调用 service-drama / service-recommend） */
    private List<Map<String, Object>> sampleDramas() {
        List<Map<String, Object>> dramas = new ArrayList<>();
        dramas.add(drama(1001L, "重生之逆袭人生", "都市励志", 98));
        dramas.add(drama(1002L, "总裁的隐藏新娘", "甜宠言情", 87));
        dramas.add(drama(1003L, "神医下山", "玄幻热血", 76));
        dramas.add(drama(1004L, "闪婚老公是首富", "都市励志", 92));
        dramas.add(drama(1005L, "王妃她不装了", "甜宠言情", 81));
        return dramas;
    }

    private Object queryDrama(String keyword) {
        // 示例数据：真实环境替换为 service-drama Feign 调用
        List<Map<String, Object>> dramas = sampleDramas();
        if (keyword == null || keyword.isBlank()) {
            return dramas;
        }
        List<Map<String, Object>> hit = new ArrayList<>();
        for (Map<String, Object> d : dramas) {
            if (d.get("title").toString().contains(keyword)) {
                hit.add(d);
            }
        }
        return hit;
    }

    private Object queryUser(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Map.of("error", "keyword required");
        }
        // 示例数据
        return Map.of(
                "userId", 10001L,
                "phone", "13800001111",
                "nickname", keyword.contains("138") ? "小兔" : "示例用户",
                "status", "正常"
        );
    }

    /**
     * 短剧推荐：有关键词时按题材推荐相似短剧，无关键词时返回热门推荐。
     * 真实环境应调用 service-recommend 的推荐接口，这里用内存数据演示推荐逻辑。
     */
    private Object recommendDrama(String keyword) {
        List<Map<String, Object>> all = sampleDramas();
        List<Map<String, Object>> result = new ArrayList<>();

        if (keyword == null || keyword.isBlank()) {
            // 无参考：按热度排序返回前 3（热门推荐）
            all.sort((a, b) -> Integer.compare((int) b.get("hotScore"), (int) a.get("hotScore")));
            for (int i = 0; i < Math.min(3, all.size()); i++) {
                result.add(all.get(i));
            }
            return Map.of("type", "hot", "list", result);
        }

        // 有参考：找出题材相同的短剧作为相似推荐（协同过滤的简化版）
        String targetCategory = null;
        for (Map<String, Object> d : all) {
            if (d.get("title").toString().contains(keyword)) {
                targetCategory = (String) d.get("category");
                break;
            }
        }
        if (targetCategory == null) {
            // 未匹配到具体剧名时，把关键词当作题材处理
            targetCategory = keyword;
        }
        for (Map<String, Object> d : all) {
            if (targetCategory.equals(d.get("category"))
                    && !d.get("title").toString().contains(keyword)) {
                result.add(d);
            }
        }
        if (result.isEmpty()) {
            return Map.of("type", "similar", "category", targetCategory, "list", all,
                    "note", "未找到同题材短剧，返回全部");
        }
        return Map.of("type", "similar", "category", targetCategory, "list", result);
    }

    /** 短剧详情 */
    private Object getDramaDetail(String dramaId) {
        for (Map<String, Object> d : sampleDramas()) {
            if (d.get("id").toString().equals(dramaId)) {
                Map<String, Object> detail = new LinkedHashMap<>(d);
                detail.put("episodes", 24);
                detail.put("intro", "《" + d.get("title") + "》是一部" + d.get("category") + "题材短剧。");
                return detail;
            }
        }
        return Map.of("error", "短剧不存在: " + dramaId);
    }

    /** 热门榜单 */
    private Object getHotRanking(String size) {
        int n = 10;
        try {
            if (size != null && !size.isBlank()) {
                n = Integer.parseInt(size);
            }
        } catch (NumberFormatException ignored) {
            // 非法入参用默认值
        }
        List<Map<String, Object>> all = sampleDramas();
        all.sort((a, b) -> Integer.compare((int) b.get("hotScore"), (int) a.get("hotScore")));
        List<Map<String, Object>> top = new ArrayList<>();
        for (int i = 0; i < Math.min(n, all.size()); i++) {
            Map<String, Object> rank = new LinkedHashMap<>(all.get(i));
            rank.put("rank", i + 1);
            top.add(rank);
        }
        return top;
    }

    private Object sendNotice(String userId, String content) {
        if (userId == null || content == null || content.isBlank()) {
            return Map.of("error", "userId and content required");
        }
        // 示例：真实环境调用 service-push / service-interaction
        return Map.of("success", true, "userId", userId, "message", "通知已发送: " + content);
    }

    // ---------- 工具方法 ----------
    private Map<String, Object> describe(String name, String desc, Map<String, String> params) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", desc);
        m.put("parameters", params);
        return m;
    }

    private Map<String, Object> drama(Long id, String title, String category, int hot) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("title", title);
        m.put("category", category);
        m.put("hotScore", hot);
        return m;
    }

    private String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? "" : v.toString();
    }
}
