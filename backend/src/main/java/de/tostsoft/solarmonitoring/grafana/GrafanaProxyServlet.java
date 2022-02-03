package de.tostsoft.solarmonitoring.grafana;

import de.tostsoft.solarmonitoring.model.User;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Acting as a management panel for Grafana
 */
public class GrafanaProxyServlet extends ProxyServlet {
  @Override
  protected HttpResponse doExecute(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
      HttpRequest proxyRequest) throws IOException, ResponseStatusException {

    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    proxyRequest.setHeader("Auth", "generated "+user.getName());

    return super.doExecute(servletRequest, servletResponse, proxyRequest);
  }
}