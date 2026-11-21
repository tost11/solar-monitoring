package de.tostsoft.solarmonitoring.app;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

public final class ClientIp {

    private ClientIp() {
    }

    public static String resolve(HttpServletRequest request, boolean trustForwardedHeaders) {
        if (trustForwardedHeaders) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.isNotEmpty(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
