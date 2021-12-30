package de.tostsoft.solarmonitoring.grafana;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.mitre.dsmiley.httpproxy.ProxyServlet;
import org.springframework.web.server.ResponseStatusException;

/**
 * Acting as a management panel for Grafana
 */
public class GrafanaProxyServlet extends ProxyServlet {

  @Override
  protected HttpResponse doExecute(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
      HttpRequest proxyRequest) throws IOException, ResponseStatusException {
    
    Header[] headers = proxyRequest.getHeaders("authorization");
    if (headers != null && headers.length > 0) {
      for (Header h : headers) {
        proxyRequest.removeHeader(h);
      }
    }

    /*headers = proxyRequest.getHeaders("Accept");
    if (headers != null && headers.length > 0) {
      for (Header h : headers) {
        proxyRequest.removeHeader(h);
      }
    }

    proxyRequest.setHeader("Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp");*/

    return super.doExecute(servletRequest, servletResponse, proxyRequest);
  }
}