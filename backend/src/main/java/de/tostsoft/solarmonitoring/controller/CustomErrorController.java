package de.tostsoft.solarmonitoring.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
public class CustomErrorController extends BasicErrorController {

  public CustomErrorController(ErrorAttributes errorAttributes,
      ErrorProperties errorProperties) {
    super(errorAttributes, errorProperties);
  }

  @Override
  public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
    if (!request.getServletPath().startsWith("/api/")) {
      return new ModelAndView("forward:/");
    } else {
      return super.errorHtml(request, response);
    }
  }
}