package com.bunny.flix.coin.config;

import com.bunny.flix.security.UserContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 用户上下文过滤器
 * 网关鉴权后把用户信息放在请求头（X-User-Id / X-User-Phone / X-User-Type）传过来，
 * 这里提取并存入 ThreadLocal（UserContext），业务代码随时可取当前用户。
 * 注意：必须在 finally 中 remove，线程池复用线程不清理会导致用户串号。
 */
@Component
public class CoinContextFilter implements Filter {

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
