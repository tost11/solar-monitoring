package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.JwtUtil;
import de.tostsoft.solarmonitoring.dtos.UserLoginDTO;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class UserService implements UserDetailsService {

  @Autowired
  private AuthenticationManager authenticationManager;
  @Autowired
  private PasswordEncoder passwordEncoder;
  @Autowired
  private UserRepository userReposetory;
  private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

  @Autowired
  private JwtUtil jwtTokenUnit;

  private void checkFixNewUserDTO(User user) {
    user.setPassword(StringUtils.trim(user.getPassword()));
    user.setName(StringUtils.trim(user.getName()));
    LOG.debug("Trim Password and UserName");
  }


  public String loginUser(UserLoginDTO userLoginDTO) {
    var authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(userLoginDTO.getName(), userLoginDTO.getPassword()));
    var user = (User) authentication.getPrincipal();
    return jwtTokenUnit.generateToken(user);
  }

  public String registerUser(UserLoginDTO userLoginDTO) {
    User user = new User(userLoginDTO.getName(), userLoginDTO.getPassword());
    checkFixNewUserDTO(user);
    user.setPassword(passwordEncoder.encode(userLoginDTO.getPassword()));
    user = userReposetory.save(user);
    System.out.println(user);
    return jwtTokenUnit.generateToken(user);
  }

  public boolean isUserAlreadyExists(UserLoginDTO userLoginDTO) {
    userReposetory.countByNameIgnoreCase(userLoginDTO.getName());
    return userReposetory.countByNameIgnoreCase(userLoginDTO.getName()) != 0;
  }

  //this function is called by authenticator and by login (is also the check for password because user is a UserDetail interface)
  @Override
  public User loadUserByUsername(String name) {
    return userReposetory.findByNameIgnoreCase(name).orElse(null);
  }

}
