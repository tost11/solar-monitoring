package de.tostsoft.solarmonitoring;

import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private JwtUtil jwtUtil;

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
      var name = jwtUtil.extractUsername(jwt);
      if (name != null) {
        User user = this.userRepository.findByNameIgnoreCase(name);
        if (user != null && jwtUtil.validateToken(jwt, user)) {
          UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
              user, null, user.getAuthorities());
          //when this here works user is authenticated
          usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        }
      }
    }
    //if nothin is changed only endpoints without restrictions are possible to use
    chain.doFilter(request, response);
  }
}
