package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private JwtUtil jwtUtil;

  private Logger LOG = LoggerFactory.getLogger(this.getClass());


  //called on every request before the ant matcher is used
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      chain.doFilter(request, response);
      return;
    }

    final var cookie = WebUtils.getCookie(request, "jwt");

    if (cookie != null && cookie.getValue() != null /*&& cookie.getValue().startsWith("Bearer ")*/) {
      //var jwt = cookie.getValue().substring(7);
      var jwt = cookie.getValue();
      try{
        var name = jwtUtil.extractUsername(jwt);
        if (name != null) {
          var user = this.userRepository.findByName(StringUtils.lowerCase(name));
          if (user != null && jwtUtil.validateToken(jwt, user)) {
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities());
            //when this here works user is authenticated
            usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
          }
        }
      }catch(SignatureException e){
        LOG.info("Exception on sign in with jwt token -> user not authorized: ",e);
      }
    }
    //if nothing is changed only endpoints without restrictions are possible to use
    chain.doFilter(request, response);
  }
}
