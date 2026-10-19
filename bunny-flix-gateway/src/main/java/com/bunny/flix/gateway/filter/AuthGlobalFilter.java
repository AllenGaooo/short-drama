package com.bunny.flix.gateway.filter;

import com.bunny.flix.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 全局登录鉴权过滤器
 * 所有请求进入网关后，先经过这个过滤器：
 * 1. 白名单路径直接放行（登录、注册、搜索等不需要登录的接口）
 * 2. 其他路径检查 Token，没有 Token 或 Token 无效则返回 401
 * 3. Token 有效时，把用户信息放到请求头中传给下游服务
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 白名单：这些路径不需要登录就能访问
     *
     * 格式说明：
     *   - 纯路径（如 "/api/search"）：按前缀匹配，不区分请求方法
     *   - "方法:路径"（如 "GET:/api/drama/list"）：仅放行该方法的请求
     *
     * ⚠️ 为什么浏览类接口要加方法限定？
     *    白名单是前缀匹配，若只写 "/api/drama/tag"，
     *    则 POST /api/drama/tag（创建标签）、DELETE /api/drama/tag/{id}（删除标签）
     *    也会被游客放行，造成越权。用 "GET:" 限定后，写操作仍需鉴权。
     */
    private static final List<String> WHITE_LIST = List.of(
            "/api/user/login",         // 登录
            "/api/user/sms",           // 发送验证码
            "/api/user/register",      // 注册
            "/api/admin/login",         // 管理员登录
            "/api/admin/register",      // 管理员注册
            "/api/search",             // 搜索
            "/api/recommend/hot",      // 热门推荐
            "/api/agent",              // 智能助手对话（无需登录）
            // 短剧浏览类接口：游客可看列表/榜单/分类，但写操作仍需登录
            "GET:/api/drama/list",     // 短剧分页列表（首页滚动加载）
            "GET:/api/drama/ranking",  // 热门榜单
            "GET:/api/drama/tag"       // 分类/标签（仅 GET，避免创建/删除标签被放行）
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "";

        // 白名单路径跳过鉴权
        if (isWhiteListed(path, method)) {
            return chain.filter(exchange);
        }

        // 从请求头中获取 Token（格式：Bearer xxxxx）
        String token = extractToken(request);
        if (token == null || token.isEmpty()) {
            return unauthorized(exchange, "请先登录");
        }

        // 验证 Token 是否有效
        try {
            if (!jwtUtils.validateToken(token)) {
                return unauthorized(exchange, "Token已过期，请重新登录");
            }

            // 从 Token 中解析用户信息，放到请求头中传给下游微服务
            Long userId = jwtUtils.getUserId(token);
            String phone = jwtUtils.getPhone(token);
            String userType = jwtUtils.getUserType(token);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-User-Phone", phone != null ? phone : "")
                    .header("X-User-Type", userType != null ? userType : "user")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return unauthorized(exchange, "Token无效");
        }
    }

    /**
     * 判断请求是否命中白名单
     *
     * @param path   请求路径
     * @param method 请求方法（GET/POST/...）
     */
    private boolean isWhiteListed(String path, String method) {
        return WHITE_LIST.stream().anyMatch(item -> {
            int idx = item.indexOf(':');
            if (idx > 0) {
                // 带方法限定的条目："GET:/api/drama/list"
                String allowMethod = item.substring(0, idx);
                String allowPath = item.substring(idx + 1);
                return allowMethod.equalsIgnoreCase(method) && path.startsWith(allowPath);
            }
            // 纯路径条目：仅按前缀匹配
            return path.startsWith(item);
        });
    }

    /** 从请求头中提取 Token，格式为 "Bearer xxxxx" */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // 去掉 "Bearer " 前缀
        }
        return null;
    }

    /** 返回 401 未授权响应 */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /** 过滤器优先级，值越小越先执行，-100 保证在大多数过滤器之前执行 */
    @Override
    public int getOrder() {
        return -100;
    }
}
