package com.example.toyrobot.api;

import com.example.toyrobot.constant.AppConstants.LogMessages;
import com.example.toyrobot.constant.AppConstants.Tracing;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Outermost filter: gives every request an id (reusing a safe {@code X-Request-Id} from the client),
 * puts it in the MDC so every log line in every layer carries it, echoes it back, and logs each
 * request's start, status and duration.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile(Tracing.REQUEST_ID_PATTERN);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = requestId(request.getHeader(Tracing.REQUEST_ID_HEADER));
        MDC.put(Tracing.MDC_REQUEST_ID, requestId);
        response.setHeader(Tracing.REQUEST_ID_HEADER, requestId);
        String method = request.getMethod();
        String uri = request.getRequestURI();
        long start = System.nanoTime();
        log.info(LogMessages.REQUEST_STARTED, method, uri);
        try {
            chain.doFilter(request, response);
            log.info(LogMessages.REQUEST_COMPLETED, method, uri, response.getStatus(), elapsedMillis(start));
        } catch (IOException | ServletException | RuntimeException e) {
            log.error(LogMessages.REQUEST_FAILED, method, uri, elapsedMillis(start), e);
            throw e;
        } finally {
            MDC.remove(Tracing.MDC_REQUEST_ID);
        }
    }

    private static String requestId(String supplied) {
        return supplied != null && SAFE_REQUEST_ID.matcher(supplied).matches() ? supplied : UUID.randomUUID().toString();
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
