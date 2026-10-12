package com.example.toyrobot.api;

import com.example.toyrobot.constant.AppConstants.ErrorCodes;
import com.example.toyrobot.constant.AppConstants.ErrorMessages;
import com.example.toyrobot.constant.AppConstants.Limits;
import com.example.toyrobot.constant.AppConstants.LogMessages;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects request bodies over {@link #MAX_BODY_BYTES}. Declared lengths are refused up front;
 * chunked bodies are cut off while being read, which surfaces as {@link BodyTooLargeException}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestSizeLimitFilter.class);

    public static final int MAX_BODY_BYTES = Limits.MAX_BODY_BYTES;

    private final ObjectMapper objectMapper;

    public RequestSizeLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static class BodyTooLargeException extends IOException {
        BodyTooLargeException() {
            super(ErrorMessages.BODY_TOO_LARGE.formatted(MAX_BODY_BYTES));
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            log.warn(LogMessages.BODY_TOO_LARGE_DECLARED, request.getMethod(), request.getRequestURI(),
                    request.getContentLengthLong(), MAX_BODY_BYTES);
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(
                    ErrorCodes.PAYLOAD_TOO_LARGE, ErrorMessages.BODY_TOO_LARGE.formatted(MAX_BODY_BYTES)));
            return;
        }
        chain.doFilter(new LimitedRequest(request), response);
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {

        private ServletInputStream stream;

        LimitedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) {
                stream = new LimitedInputStream(super.getInputStream());
            }
            return stream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private long count;

        LimitedInputStream(ServletInputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b != -1) {
                count(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = delegate.read(buffer, offset, length);
            if (n > 0) {
                count(n);
            }
            return n;
        }

        private void count(int n) throws BodyTooLargeException {
            count += n;
            if (count > MAX_BODY_BYTES) {
                log.warn(LogMessages.BODY_TOO_LARGE_STREAMED, MAX_BODY_BYTES);
                throw new BodyTooLargeException();
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }
    }
}
