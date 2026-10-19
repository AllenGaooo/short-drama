package com.bunny.flix.aigateway.config;

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
 * 用户上下文过滤器：把网关透传的 X-User-Id 还原进 ThreadLocal
 */
@Component
public class AiGatewayContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String userIdHeader = httpRequest.getHeader("X-User-Id");
        String phoneHeader = httpRequest.getHeader("X-User-Phone");
        String userTypeHeader = httpRequest.getHeader("X-User-Type");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            UserContext ctx = new UserContext();
            ctx.setUserId(Long.valueOf(userIdHeader));
            ctx.setPhone(phoneHeader);
            ctx.setUserType(userTypeHeader);
            UserContext.set(ctx);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.remove();
        }
    }
}
