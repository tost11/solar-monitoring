package de.tostsoft.solarmonitoring.configuration;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ErrorConfiguration {
  @Bean
  public ErrorAttributes errorAttributes(){
    return new DefaultErrorAttributes();
  }

  @Bean
  public ErrorProperties errorProperties(){
    return new ErrorProperties();
  }

}
