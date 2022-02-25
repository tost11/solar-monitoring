package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.JwtUtil;
import de.tostsoft.solarmonitoring.dtos.UserDTO;
import de.tostsoft.solarmonitoring.dtos.UserLoginDTO;
import de.tostsoft.solarmonitoring.dtos.UserRegisterDTO;
import de.tostsoft.solarmonitoring.dtos.UserTableRowDTO;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

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

    @PostConstruct
    void initUserConstrain(){
        //TODO check if this here is working
        userRepository.initNameConstrain();
    }

    private void checkFixNewUserDTO(User user) {
        user.setPassword(StringUtils.trim(user.getPassword()));
        user.setName(StringUtils.trim(user.getName()));
        LOG.debug("Trim Password and UserName");
    }


    public UserDTO loginUser(UserLoginDTO userLoginDTO) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userLoginDTO.getName(), userLoginDTO.getPassword()));
        var user = (User) authentication.getPrincipal();
        String jwt = jwtTokenUnit.generateToken(user);
        UserDTO userDTO = new UserDTO(user.getId(),userLoginDTO.getName());
        userDTO.setJwt(jwt);
        userDTO.setAdmin(user.isAdmin());
        return userDTO;
    }

    public UserDTO registerUser(UserRegisterDTO userRegisterDTO) {

        Set<String> labels= new HashSet<>();
        labels.add(Neo4jLabels.User.toString());
        labels.add(Neo4jLabels.NOT_FINISHED.toString());

        User user = User.builder()
            .name(userRegisterDTO.getName())
            .creationDate(Instant.now())
            .numAllowedSystems(3)
            .labels(labels)
            .build();

        user = userRepository.save(user);

        String generatedName = "user-"+user.getId();

        influxConnection.createNewBucket(generatedName);

        user.setGrafanaUserId(grafanaService.createNewUser(generatedName,user.getName()));
        user.setGrafanaFolderId(grafanaService.createFolder(generatedName,generatedName).getId());
        grafanaService.setPermissionsForFolder(user.getGrafanaUserId(),generatedName);

        user.getLabels().remove(Neo4jLabels.NOT_FINISHED.toString());
        user.setPassword(passwordEncoder.encode(userRegisterDTO.getPassword()));

        labels.remove(Neo4jLabels.NOT_FINISHED.toString());
        user.setLabels(labels);

        user = userRepository.save(user);

        LOG.info("Created new user with name: {}",user.getName());

        UserDTO userDTO= new UserDTO(user.getId(),user.getName());
        userDTO.setJwt(jwtTokenUnit.generateToken(user));
        return userDTO;
    }

    public boolean isUserAlreadyExists(UserRegisterDTO userRegisterDTO) {
        userRepository.countByNameIgnoreCase(userRegisterDTO.getName());
        return userRepository.countByNameIgnoreCase(userRegisterDTO.getName()) != 0;
    }

    public ResponseEntity<UserDTO> patchUser(UserDTO userDTO){
        User user = userRepository.findUserById(userDTO.getId());
        user.setAdmin(userDTO.isAdmin());
        user.setNumAllowedSystems(userDTO.getNumAllowedSystems());
        user=userRepository.save(user);
        UserDTO responseUserDTO= new UserDTO(user.getId(),user.getName());
        return ResponseEntity.status(HttpStatus.OK).body(responseUserDTO);
    }

    public List<UserTableRowDTO> findUser(String name) {

        List<User> userList = userRepository.findAllInitializedAndAdminStartsWith(name);
        List<UserTableRowDTO> userDTOS = new ArrayList<>();
        for(User user:userList){
            UserTableRowDTO userDTO= new UserTableRowDTO(user.getId(),user.getName(),user.getNumAllowedSystems(),user.isAdmin());
            userDTOS.add(userDTO);
        }
        return userDTOS;
    }
}
