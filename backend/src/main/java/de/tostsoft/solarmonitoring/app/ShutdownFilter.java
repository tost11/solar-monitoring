package de.tostsoft.solarmonitoring.app;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ShutdownFilter extends OncePerRequestFilter {

    private final Logger LOG = LoggerFactory.getLogger(ShutdownFilter.class);

    boolean isShuttingDown = false;

    @EventListener({ ContextClosedEvent.class })
    public void onApplicationEvent(ContextClosedEvent event) {
        LOG.info("ShutdownFilter handling shutdown request");
        isShuttingDown = true;
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        LOG.debug( "shutdown filter triggered");

        if(isShuttingDown){
            (response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "application is shutting down");
            return;
        }
        //continue with filters
        chain.doFilter(request, response);
    }
}
