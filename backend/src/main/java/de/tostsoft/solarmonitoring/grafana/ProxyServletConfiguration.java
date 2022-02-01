package de.tostsoft.solarmonitoring.grafana;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyServletConfiguration {

  /**
   * Read the routing settings in the configuration file
   */
  @Value("${proxy.grafana.servlet.url}")
  private String servlet_url;

  /**
   * Read the proxy target address in the configuration
   */
  @Value("${proxy.grafana.target.url}")
  private String target_url;

  @Bean
  public ServletRegistrationBean proxyServletRegistration() {
    ServletRegistrationBean registrationBean = new ServletRegistrationBean(new GrafanaProxyServlet(), servlet_url);
    //Set URL and parameters
    Map<String, String> params = Map.of("targetUri", target_url, "log", "true");
    registrationBean.setInitParameters(params);
    return registrationBean;
  }

}