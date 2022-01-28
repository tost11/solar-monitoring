package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.JwtUtil;
import de.tostsoft.solarmonitoring.dtos.UserDTO;
import de.tostsoft.solarmonitoring.dtos.UserLoginDTO;
import de.tostsoft.solarmonitoring.dtos.UserRegisterDTO;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import lombok.Synchronized;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private GrafanaService grafanaService;

    @Autowired
    private JwtUtil jwtTokenUnit;

    @Autowired
    private InfluxConnection influxConnection;

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

    public UserDTO registerUser(UserRegisterDTO userRegisterDTO) {

        return registerUser(userRegisterDTO, null);
    }

    //TODO implement rollback or cleanup mechanism if neo4j adding of user fails to cleanup unused user in grafana
    @Synchronized
    public UserDTO registerUser(UserRegisterDTO userRegisterDTO,String grafanFolderUid) {

        String dependencyUsername = "generated "+userRegisterDTO.getName();
        //create user in grafana
        var resp = grafanaService.createNewUser(dependencyUsername,grafanFolderUid);

        influxConnection.createNewBucket(dependencyUsername);

        User user = new User(userRegisterDTO.getName(), userRegisterDTO.getPassword(),resp.id,resp.folderUuid);
        checkFixNewUserDTO(user);
        user.setPassword(passwordEncoder.encode(userRegisterDTO.getPassword()));

        user = userRepository.save(user);

        LOG.info(user.toString());

        UserDTO userDTO= new UserDTO(user.getName());
        userDTO.setJwt(jwtTokenUnit.generateToken(user));
        return userDTO;
    }

    public boolean isUserAlreadyExists(UserRegisterDTO userRegisterDTO) {
        userRepository.countByNameIgnoreCase(userRegisterDTO.getName());
        return userRepository.countByNameIgnoreCase(userRegisterDTO.getName()) != 0;
    }


    //this function is called by authenticator and by login (is also the check for password because user is a UserDetail interface)
    @Override
    public User loadUserByUsername(String name) {
        return userRepository.findByNameIgnoreCase(name);
    }
}
