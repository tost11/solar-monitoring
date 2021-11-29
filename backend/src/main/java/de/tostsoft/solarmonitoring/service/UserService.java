package de.tostsoft.solarmonitoring.service;
/*
import de.tostsoft.solarmonitoring.JwtUtil;
import de.tostsoft.solarmonitoring.dtos.UserLoginDTO;
import de.tostsoft.solarmonitoring.module.User;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;



public class UserService implements UserDetailsService {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private JwtUtil jwtTokenUnit;

    private void registeCheck(User user) {
        user.setPassword(StringUtils.trim(user.getPassword()));
        user.setName(StringUtils.trim(user.getName()));
        LOG.info("Trim Password and UserName");
    }



    public ResponseEntity loginMachCheck(UserLoginDTO userLoginDTO) throws Exception {
        try {
            var name = userRepository.findByNameIgnoreCase(userLoginDTO.getName());
            if (userLoginDTO.getName().equals(name)) {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(userLoginDTO.getName(), userLoginDTO.getPassword()));

            }
        } catch (Exception e) {
          //  throw new AuthenticationError("Incorrect user ore Password");

        }

        final User user = loadUserByUsername(userLoginDTO.getName());
        final String jwt = jwtTokenUnit.generateToken(user);
        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }

    public ResponseEntity<AuthenticationResponse> userRegister(UserRegisterDTO userRegisterDTO) throws Exception {


        try {
            User user = new User(userRegisterDTO.getName(), userRegisterDTO.getPassword(), userRegisterDTO.getBirthday());
            registeCheck(user);
            user.setPassword(passwordEncoder.encode(userRegisterDTO.getPassword()));
            final String jwt = jwtTokenUnit.generateToken(user);
            userRepository.save(user);
            return ResponseEntity.ok(new AuthenticationResponse(jwt));
        } catch (Exception e) {
            throw new Exception("User can't save");
        }


    }

    public boolean isUserAlreadyExists(UserRegisterDTO userRegisterDTO) {
        return userRepository.countByNameIgnoreCase(userRegisterDTO.getName()) != 0;
    }

    public User getUserByName(String name) {
        // User user = new User();
        // userRepository.update(getUserByName(name), user);
        return userRepository.findByNameIgnoreCase(name);
    }

    @Override
    public User loadUserByUsername(String name) throws UsernameNotFoundException {

        return userRepository.findByNameIgnoreCase(name);
    }
}
*/