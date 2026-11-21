package de.tostsoft.solarmonitoring.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tostsoft.solarmonitoring.app.service.BruteForceProtectionService;
import de.tostsoft.solarmonitoring.lib.dtos.ApiErrorResponseDTO;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class BruteForceFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(BruteForceFilter.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public record Rule(String method, String path, String keyField, String keyPrefix, boolean resolveUserForMail) {
    }

    private final BruteForceProtectionService bruteForceProtectionService;
    private final UserRepository userRepository;
    private final List<Rule> rules;
    private final boolean trustForwardedHeaders;

    public BruteForceFilter(BruteForceProtectionService bruteForceProtectionService,
                            UserRepository userRepository,
                            List<Rule> rules,
                            boolean trustForwardedHeaders) {
        this.bruteForceProtectionService = bruteForceProtectionService;
        this.userRepository = userRepository;
        this.rules = rules;
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Rule rule = match(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        String keyValue = extractKey(wrappedRequest, rule.keyField());
        if (StringUtils.isBlank(keyValue)) {
            // malformed request: let the controller answer (400) without recording an attempt
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        String key = rule.keyPrefix() + keyValue;
        String ip = ClientIp.resolve(wrappedRequest, trustForwardedHeaders);
        String userAgent = wrappedRequest.getHeader("User-Agent");

        try {
            bruteForceProtectionService.checkLoginAttempt(key, ip);
        } catch (ResponseStatusException e) {
            writeError(response, e.getReason(), e.getStatusCode());
            return;
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            recordOutcome(rule, key, ip, userAgent, wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private Rule match(HttpServletRequest request) {
        for (Rule rule : rules) {
            if (rule.method().equalsIgnoreCase(request.getMethod()) && rule.path().equals(request.getRequestURI())) {
                return rule;
            }
        }
        return null;
    }

    private String extractKey(CachedBodyHttpServletRequest request, String field) {
        byte[] body = request.getCachedBody();
        if (body.length == 0) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(body);
            JsonNode value = node.get(field);
            return value != null && value.isTextual() ? value.asText() : null;
        } catch (IOException e) {
            return null;
        }
    }

    private void recordOutcome(Rule rule, String key, String ip, String userAgent, ContentCachingResponseWrapper response) {
        int status = response.getStatus();
        boolean failed;
        if (status >= 400 && status < 500) {
            failed = true;
        } else if (status == 200) {
            failed = bodyHasValidFalse(response.getContentAsByteArray());
        } else if (status >= 500) {
            return;
        } else {
            failed = false;
        }

        if (failed) {
            User existingUser = rule.resolveUserForMail()
                    ? userRepository.findByName(StringUtils.lowerCase(StringUtils.trim(key)))
                    : null;
            bruteForceProtectionService.recordFailedLogin(key, ip, userAgent, existingUser);
        } else {
            bruteForceProtectionService.recordSuccessfulLogin(key, ip, userAgent);
        }
    }

    private boolean bodyHasValidFalse(byte[] body) {
        if (body == null || body.length == 0) {
            return false;
        }
        try {
            JsonNode valid = OBJECT_MAPPER.readTree(body).get("valid");
            return valid != null && valid.isBoolean() && !valid.asBoolean();
        } catch (IOException e) {
            return false;
        }
    }

    private void writeError(HttpServletResponse response, String reason, org.springframework.http.HttpStatusCode status) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        OBJECT_MAPPER.writeValue(response.getWriter(), new ApiErrorResponseDTO(reason, status, new Date()));
    }

    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = StreamUtils.copyToByteArray(request.getInputStream());
        }

        byte[] getCachedBody() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), StandardCharsets.UTF_8));
        }

        private static final class CachedBodyServletInputStream extends ServletInputStream {

            private final ByteArrayInputStream delegate;

            CachedBodyServletInputStream(byte[] body) {
                this.delegate = new ByteArrayInputStream(body);
            }

            @Override
            public boolean isFinished() {
                return delegate.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int read() {
                return delegate.read();
            }
        }
    }
}
