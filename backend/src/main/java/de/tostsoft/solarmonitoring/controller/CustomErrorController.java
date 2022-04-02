package de.tostsoft.solarmonitoring.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Component
public class CustomErrorController extends BasicErrorController {

  public CustomErrorController(ErrorAttributes errorAttributes) {
    super(errorAttributes, new ErrorProperties());
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