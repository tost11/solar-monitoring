package de.tostsoft.solarmonitoring.app;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class ViewTokenSupport {

    public static final String HEADER = "X-View-Token";

    private ViewTokenSupport() {
    }

    public static String currentToken() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest().getHeader(HEADER);
        }
        return null;
    }
}
