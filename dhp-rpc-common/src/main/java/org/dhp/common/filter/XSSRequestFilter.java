package org.dhp.common.filter;

import org.springframework.http.HttpMethod;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class XSSRequestFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if(request instanceof HttpServletRequest) {
            //GET 请求无视XSS
            if(((HttpServletRequest) request).getMethod().equals(HttpMethod.GET)) {
                chain.doFilter(request, response);
                return;
            }
        }
        chain.doFilter(new XSSRequestWrapper((HttpServletRequest) request), response);
    }

}
