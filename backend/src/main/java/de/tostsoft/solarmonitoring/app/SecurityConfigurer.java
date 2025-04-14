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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfigurer implements UserDetailsService {

  @Autowired
  private CustomOAuth2AuthenticationSuccessHandler customOAuth2AuthenticationSuccessHandler;

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
            "/api/login/**",
            "/api/status/**",
            "/api/tags/systems",
            "/api/tags/byIds",
            "/api/system/public/**",
            "/api/system/search",
            "/api/influx/**",
            "/api/proxy/**"
        ).permitAll()
        .requestMatchers("/api/**").authenticated()
        .anyRequest().permitAll()
        .and()
        .oauth2Login(oauth->{
          oauth.loginProcessingUrl("/api/login/oauth2/callback");
          oauth.successHandler(customOAuth2AuthenticationSuccessHandler);
          oauth.failureHandler(new
                  SimpleUrlAuthenticationFailureHandler("/login?error=true"));
        });


    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

    http.headers().frameOptions().sameOrigin();
    return http.build();
  }



}
