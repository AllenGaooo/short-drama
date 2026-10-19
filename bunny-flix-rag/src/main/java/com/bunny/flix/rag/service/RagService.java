package com.bunny.flix.rag.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 极简 RAG 检索实现（无需外部向量库）
 * - 使用词频（TF）向量表示文本
 * - 用余弦相似度做召回
 * 真实生产中可替换为 Elasticsearch / Milvus / 第三方 Embedding 服务。
 */
@Service
public class RagService {

    private final List<Map<String, Object>> store = new ArrayList<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @PostConstruct
    public void init() {
        // 预置兔兔短剧业务知识库
        ingest(List.of(
                Map.of("content", "兔兔短剧是一个面向移动端的短剧分发平台，提供都市、甜宠、玄幻等多类短剧。"),
                Map.of("content", "用户可以通过手机号注册登录，支持验证码登录与密码登录两种方式。"),
                Map.of("content", "会员体系包含普通会员与超级会员，超级会员可免费观看全部短剧。"),
                Map.of("content", "优惠券由营销服务发放，可用于抵扣会员费用或单集解锁费用。"),
                Map.of("content", "推荐服务根据用户播放行为实时更新个性化短剧推荐列表。"),
                Map.of("content", "运营后台可管理短剧上下架、用户冻结、优惠券发放等日常运营操作。")
        ));
    }

    public void ingest(List<Map<String, Object>> docs) {
        for (Map<String, Object> d : docs) {
            Object c = d.get("content");
            if (c == null) continue;
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("id", idGen.getAndIncrement());
            doc.put("content", c.toString());
            doc.put("vector", vectorize(c.toString()));
            store.add(doc);
        }
    }

    public List<Map<String, Object>> retrieve(String query, int topK) {
        double[] qv = vectorize(query);
        List<Map<String, double[]>> scored = new ArrayList<>();
        for (Map<String, Object> doc : store) {
            scored.add(Map.of("score", cosine(qv, (double[]) doc.get("vector")), "doc", doc));
        }
        scored.sort((a, b) -> Double.compare(
                ((Number) b.get("score")).doubleValue(),
                ((Number) a.get("score")).doubleValue()));

        List<Map<String, Object>> result = new ArrayList<>();
        int n = Math.min(topK, scored.size());
        for (int i = 0; i < n; i++) {
            @SuppressWarnings("unchecked")
            Map<String, Object> doc = (Map<String, Object>) scored.get(i).get("doc");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("content", doc.get("content"));
            item.put("score", scored.get(i).get("score"));
            result.add(item);
        }
        return result;
    }

    // ---------- 向量与相似度 ----------
    private double[] vectorize(String text) {
        Map<String, Integer> tf = new ConcurrentHashMap<>();
        // 简单按字符 n-gram(2) 切分，中文效果尚可
        String clean = text.replaceAll("\\s+", "");
        for (int i = 0; i < clean.length(); i++) {
            int end = Math.min(i + 2, clean.length());
            String gram = clean.substring(i, end);
            tf.merge(gram, 1, Integer::sum);
        }
        int dim = 1024;
        double[] vec = new double[dim];
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            int idx = Math.floorMod(e.getKey().hashCode(), dim);
            vec[idx] += e.getValue();
        }
        return vec;
    }

    private double cosine(double[] a, double[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
