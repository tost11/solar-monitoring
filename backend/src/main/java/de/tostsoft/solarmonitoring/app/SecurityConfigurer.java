package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfigurer implements UserDetailsService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var user = userRepository.findByName(username);
    return user;
    /*User u = new User();
    u.setId(user.getId());
    u.setName(user.getName());
    u.setPassword(user.getPassword());
    u.setIsAdmin(user.getIsAdmin());
    u.setGrafanaUserId(user.getGrafanaUserId());
    u.setNumAllowedSystems(user.getNumAllowedSystems());
    return u;*/
  }

  @Autowired
  private JwtRequestFilter jwtRequestFilter;

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
    authenticationProvider.setUserDetailsService(this);
    authenticationProvider.setPasswordEncoder(passwordEncoder);
    return authenticationProvider;
  }

  @Bean
  @Order(1)
  public SecurityFilterChain auth0FilterChain(HttpSecurity http) throws Exception {
    http.csrf().disable();
    http.anonymous().disable();


    //IMPORTANT !!! if done changes remeber frontend ist used on extra port. check if main momain still working !!!!
    http.authorizeHttpRequests()
        .requestMatchers(
            "/api/solar/data/**",
            "/api/user/register",
            "/api/user/login",
            "/api/status/**",
            "/api/tags/**",
            "/api/system/public/**",
            "/api/influx/**",
            "/api/proxy/**"
        ).permitAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().permitAll();

    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

    http.headers().frameOptions().sameOrigin();
    return http.build();
  }
}
