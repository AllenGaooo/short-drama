package com.bunny.flix.user.config;

import com.bunny.flix.security.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 用户上下文过滤器
 * 网关把用户信息放在请求头中传过来，
 * 这个过滤器提取这些信息并存入 ThreadLocal（UserContext），
 * 这样业务代码中随时可以通过 UserContext.getUserId() 获取当前登录用户
 */
@Slf4j
@Component
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 从请求头中提取网关传过来的用户信息
        String userIdHeader = httpRequest.getHeader("X-User-Id");
        String phoneHeader = httpRequest.getHeader("X-User-Phone");
        String userTypeHeader = httpRequest.getHeader("X-User-Type");

        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            UserContext userContext = new UserContext();
            userContext.setUserId(Long.valueOf(userIdHeader));
            userContext.setPhone(phoneHeader);
            userContext.setUserType(userTypeHeader);
            UserContext.set(userContext);
        }

        try {
            // 执行业务逻辑
            chain.doFilter(request, response);
        } finally {
            // 请求结束一定清理，防止内存泄漏
            UserContext.remove();
        }
    }
}
