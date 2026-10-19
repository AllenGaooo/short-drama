package com.bunny.flix.admin.config;

import com.bunny.flix.security.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 后台用户上下文过滤器
 * 网关把管理员信息放在请求头（X-User-Id / X-User-Phone / X-User-Type）传过来，
 * 这里提取并存入 ThreadLocal（UserContext），业务代码随时可取当前管理员
 */
@Slf4j
@Component
public class AdminContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

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
            chain.doFilter(request, response);
        } finally {
            UserContext.remove();
        }
    }
}
