package com.blog.server.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * I3: XSS prevention filter — strips dangerous HTML/script from query parameters.
 * Content body (JSON) is sanitized by the application layer, not here.
 */
@Component
@Order(2)
public class XssFilter implements Filter {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]*?>.*?</script>|javascript:|on\\w+\\s*=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }

    private static class XssRequestWrapper extends HttpServletRequestWrapper {
        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return value != null ? sanitize(value) : null;
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return value != null ? sanitize(value) : null;
        }

        private String sanitize(String input) {
            if (input == null) return null;
            return SCRIPT_PATTERN.matcher(input)
                    .replaceAll("")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }
    }
}
