package com.blog.server.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * XSS prevention filter — strips dangerous HTML/script from query parameters,
 * headers, and JSON request bodies.
 * Article editing endpoints are excluded since they legitimately contain HTML.
 */
@Component
@Order(2)
@SuppressWarnings("null")
public class XssFilter implements Filter {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]*?>.*?</script>|javascript:|on\\w+\\s*=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** Paths that should NOT have their body sanitized (they contain legitimate HTML) */
    private static final String[] BODY_EXCLUDE_PATHS = {
            "/api/admin/article/create",
            "/api/admin/article/update"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Check if body sanitization should be skipped for this path
        boolean excludeBody = false;
        for (String excluded : BODY_EXCLUDE_PATHS) {
            if (path.startsWith(excluded)) {
                excludeBody = true;
                break;
            }
        }

        chain.doFilter(new XssRequestWrapper(httpRequest, excludeBody), response);
    }

    private static class XssRequestWrapper extends HttpServletRequestWrapper {
        private final boolean excludeBody;
        private byte[] cachedBody;

        public XssRequestWrapper(HttpServletRequest request, boolean excludeBody) {
            super(request);
            this.excludeBody = excludeBody;
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

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (excludeBody) {
                return super.getInputStream();
            }
            if (cachedBody == null) {
                String body = StreamUtils.copyToString(super.getInputStream(), StandardCharsets.UTF_8);
                cachedBody = sanitize(body).getBytes(StandardCharsets.UTF_8);
            }
            return new CachedServletInputStream(cachedBody);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        private String sanitize(String input) {
            if (input == null) return null;
            return SCRIPT_PATTERN.matcher(input)
                    .replaceAll("")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }
    }

    /**
     * Wrapper around a byte array to serve as a ServletInputStream.
     */
    private static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream bais;

        public CachedServletInputStream(byte[] cachedBody) {
            this.bais = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public int read() { return bais.read(); }

        @Override
        public boolean isFinished() { return bais.available() == 0; }

        @Override
        public boolean isReady() { return true; }

        @Override
        public void setReadListener(ReadListener readListener) {
            // No async support needed
        }
    }
}
