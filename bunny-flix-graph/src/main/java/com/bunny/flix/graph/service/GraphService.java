package com.bunny.flix.graph.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存知识图谱（无需外部图数据库）
 * 以 (subject, relation, object) 三元组存储兔兔短剧业务关系。
 */
@Service
public class GraphService {

    /** 三元组存储：subject -> relation -> List<object> */
    private final List<Map<String, String>> triples = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        add("重生之逆袭人生", "属于分类", "都市励志");
        add("重生之逆袭人生", "由演员", "林夕");
        add("总裁的隐藏新娘", "属于分类", "甜宠言情");
        add("神医下山", "属于分类", "玄幻热血");
        add("重生之逆袭人生", "在平台", "兔兔短剧");
        add("总裁的隐藏新娘", "在平台", "兔兔短剧");
        add("神医下山", "在平台", "兔兔短剧");
        add("兔兔短剧", "提供", "短剧分发");
        add("兔兔短剧", "拥有会员体系", "超级会员");
        add("超级会员", "权益", "免费看全部短剧");
        add("优惠券", "用于抵扣", "会员费用");
        add("优惠券", "由服务发放", "营销服务");
        add("推荐服务", "依据", "用户播放行为");
        add("用户", "可观看", "短剧");
        add("短剧", "产生收益", "平台营收");
    }

    public Map<String, Object> query(String entity, String relation) {
        List<Map<String, String>> hit = new ArrayList<>();
        for (Map<String, String> t : triples) {
            boolean eOk = entity == null || entity.isBlank() || t.get("subject").contains(entity);
            boolean rOk = relation == null || relation.isBlank() || t.get("relation").contains(relation);
            if (eOk && rOk) hit.add(t);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("entity", entity);
        out.put("relation", relation);
        out.put("triples", hit);
        out.put("count", hit.size());
        return out;
    }

    /** 自然语言：抽取实体（命中已知 subject）与关系关键词 */
    public Map<String, Object> ask(String question) {
        String entity = null;
        for (Map<String, String> t : triples) {
            if (question != null && question.contains(t.get("subject"))) {
                entity = t.get("subject");
                break;
            }
        }
        // 关系关键词
        String relation = null;
        String[] relWords = {"分类", "演员", "平台", "会员", "优惠券", "推荐", "观看", "收益", "关系", "关联"};
        if (question != null) {
            for (String w : relWords) {
                if (question.contains(w)) { relation = w; break; }
            }
        }
        return query(entity, relation);
    }

    private void add(String subject, String relation, String object) {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("subject", subject);
        t.put("relation", relation);
        t.put("object", object);
        triples.add(t);
    }
}
