package de.tostsoft.solarmonitoring.app.controller;

import de.tostsoft.solarmonitoring.app.service.ProxyService;
import de.tostsoft.solarmonitoring.lib.dtos.proxy.ProxySystemsDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

  @Value("${api.tokens.proxy:}")
  private String proxyEndpointToken;

  @Autowired
  private ProxyService proxyService;

  @GetMapping("/systems")
  public ProxySystemsDTO getSystems(@RequestHeader String proxyToken){
    if(!StringUtils.equals(proxyEndpointToken,proxyToken)){
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"token wrong");
    }
    var ret = new ProxySystemsDTO();
    ret.setSystems(proxyService.getSystems());
    return ret;
  }

}
