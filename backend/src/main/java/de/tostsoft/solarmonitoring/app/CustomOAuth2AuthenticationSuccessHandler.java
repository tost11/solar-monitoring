package de.tostsoft.solarmonitoring.app;

import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

@Configuration
public class CustomOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException,
            ServletException {

        if(authentication instanceof OAuth2AuthenticationToken){
            OAuth2User oauth2User = ((OAuth2AuthenticationToken)
                    authentication).getPrincipal();
            User customer = userRepository.findAll().get(0);
            if(customer!=null){
                System.out.println("customer exist");
                String token = jwtUtil.generateJWT(customer);
                //more process you have to do in your logic
                response.getWriter().write(token);
                response.getWriter().flush();

            }else{
                //customer doesn't exist, throw an error or create new one
            }
        }else{
            //error
        }

    }
}