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
import java.util.*;
import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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



    public UserDTO loginUser(UserLoginDTO userLoginDTO) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userLoginDTO.getName(), userLoginDTO.getPassword()));
        var user = (User) authentication.getPrincipal();
        String jwt = jwtTokenUnit.generateToken(user);
        UserDTO userDTO = new UserDTO(userLoginDTO.getName());
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
            .numbAllowedSystems(1)
            .labels(labels)
            .build();

        user = userRepository.save(user);

        String generatedName = "user-"+user.getId();

        influxConnection.createNewBucket(generatedName);

        user.setGrafanaUserId(grafanaService.createNewUser(generatedName,user.getName()));
        user.setGrafanaFolderId(grafanaService.createFolder(generatedName,generatedName).getId());
        grafanaService.setPermissionsForFolder(user.getGrafanaUserId(),generatedName);

        userRepository.save(user);

        user.setPassword(passwordEncoder.encode(userRegisterDTO.getPassword()));

        labels.remove(Neo4jLabels.NOT_FINISHED.toString());
        user.setLabels(labels);

        user = userRepository.save(user);

        LOG.info("Created new user with name: {}",user.getName());

        UserDTO userDTO= new UserDTO(user.getName());
        userDTO.setJwt(jwtTokenUnit.generateToken(user));
        return userDTO;
    }

    public boolean isUserAlreadyExists(UserRegisterDTO userRegisterDTO) {
        userRepository.countByNameIgnoreCase(userRegisterDTO.getName());
        return userRepository.countByNameIgnoreCase(userRegisterDTO.getName()) != 0;
    }

    public ResponseEntity<UserDTO> patchUser(String name,UserDTO userDTO){
        User user = userRepository.findByNameIgnoreCase(name);
        user.setAdmin(userDTO.isAdmin());
        user.setNumbAllowedSystems(userDTO.getNumbAllowedSystems());
        user=userRepository.save(user);
        UserDTO responseUserDTO= new UserDTO(user.getName());
        return ResponseEntity.status(HttpStatus.OK).body(responseUserDTO);
    }

    public List<UserTableRowDTO> getAllUser() {

        List<User> userList = userRepository.findAllInitialized();
        List<UserTableRowDTO> userDTOS = new ArrayList<>();
        for(User user:userList){
            UserTableRowDTO userDTO= new UserTableRowDTO(user.getName(),user.getNumbAllowedSystems(),user.isAdmin());
            userDTOS.add(userDTO);
        }
        return userDTOS;
    }
}
