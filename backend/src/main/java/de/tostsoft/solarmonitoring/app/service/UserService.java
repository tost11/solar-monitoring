package de.tostsoft.solarmonitoring.app.service;

import de.tostsoft.solarmonitoring.app.Converter;
import de.tostsoft.solarmonitoring.app.JwtUtil;
import de.tostsoft.solarmonitoring.app.dtos.GenericDataDTO;
import de.tostsoft.solarmonitoring.app.dtos.admin.UpdateUserForAdminDTO;
import de.tostsoft.solarmonitoring.app.dtos.admin.UserForAdminDTO;
import de.tostsoft.solarmonitoring.app.dtos.admin.UserTableRowForAdminDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserLoginDTO;
import de.tostsoft.solarmonitoring.app.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.lib.model.User;
import de.tostsoft.solarmonitoring.lib.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.lib.repository.SolarSystemRepository;
import de.tostsoft.solarmonitoring.lib.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private JwtUtil jwtTokenUnit;

    @Autowired
    private InfluxConnection influxConnection;

    @Value("${user.defaultNumSystems}")
    private int defaultNumSystems;

    @Autowired
    private SolarSystemRepository solarSystemRepository;

    public UserDTO loginUser(UserLoginDTO userLoginDTO) {
        var authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(userLoginDTO.getName(), userLoginDTO.getPassword()));
        var user = (User) authentication.getPrincipal();
        String jwt = jwtTokenUnit.generateJWT(user);
        UserDTO userDTO = new UserDTO(user.getId(), userLoginDTO.getName());
        userDTO.setJwt(jwt);
        userDTO.setAdmin(user.getIsAdmin());
        return userDTO;
    }

    public UserDTO registerUser(UserRegisterDTO userRegisterDTO) {

        var id = new ObjectId();

        var user = User.builder()
            .id(id.toString())
            .name(StringUtils.lowerCase(userRegisterDTO.getName()))
            .viewName(userRegisterDTO.getName())
            .creationDate(LocalDateTime.now())
            .influxBucketName(id.toString())
            .numAllowedSystems(defaultNumSystems)
            .password(passwordEncoder.encode(userRegisterDTO.getPassword()))
            .isAdmin(false)
            .build();

        //user = creationUserRepository.save(user);

        influxConnection.createNewBucket(user.getId());
        user.setInfluxBucketName(user.getId());

        LOG.info("Created new user with name: {}", user.getName());

        user = userRepository.save(user);

        UserDTO userDTO = new UserDTO(user.getId(), user.getName());
        userDTO.setJwt(jwtTokenUnit.generateJWT(user));
        return userDTO;
    }

    public boolean checkUsernameAlreadyTaken(UserRegisterDTO userRegisterDTO) {
        return userRepository.countByName(StringUtils.lowerCase(userRegisterDTO.getName())) != 0;
    }

    UserForAdminDTO convertUserToUserForAdminDTO(User user,boolean isDeleted) {
        return UserForAdminDTO.builder()
                .id(user.getId())
                .isAdmin(user.getIsAdmin())
                .name(user.getName())
                .numbAllowedSystems(user.getNumAllowedSystems())
                .creationDate(user.getCreationDate().atZone(ZoneId.of("UTC")))
                .isDeleted(isDeleted)
                .build();
    }

    public UserForAdminDTO editUser(UpdateUserForAdminDTO userDTO) {
        var userOpt = userRepository.seesAllFindById(userDTO.getId());
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        var user = userOpt.get();
        user.setName(userDTO.getName());
        user.setIsAdmin(userDTO.isAdmin());
        user.setNumAllowedSystems(userDTO.getNumAllowedSystems());

        if(userDTO.isDeleted()){
            user.setDeletedAt(LocalDateTime.now());
            solarSystemRepository.setDeleteAtOnAllActiveSystemsByOwner(user.getId(),LocalDateTime.now());
        }else{
            user.setDeletedAt(null);
        }
        user = userRepository.save(user);
        return convertUserToUserForAdminDTO(user,userDTO.isDeleted());
    }

    public Collection<UserTableRowForAdminDTO> findUserForAdmin(String name) {
        var lowerName = StringUtils.lowerCase(name);

        //map needet because maby user is in deleted and not deleted users at the same time (cleanup job will fix that)
        Map<String,UserTableRowForAdminDTO> userDTOS = new HashMap<>();

        List<User> userList = userRepository.seesAllFindAllByNameStartingWith(lowerName);
        for(var user : userList){
            UserTableRowForAdminDTO userDTO = new UserTableRowForAdminDTO(user.getId(),
                user.getName(),
                user.getNumAllowedSystems(),
                user.getIsAdmin(),
                user.getDeletedAt() != null);
            userDTOS.put(user.getId(),userDTO);
        }

        return userDTOS.values();
    }

    public List<GenericDataDTO> findUsers(String name) {
        List<User> userList = userRepository.findAllByNameStartingWith(StringUtils.lowerCase(name));
        return userList.stream().map(u->new GenericDataDTO(u.getId(),u.getName())).collect(Collectors.toList());
    }

    public boolean isUserFromContextAdmin(){
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.countByIdAndIsAdmin(user.getId(),true) > 0;
    }

    public User getLoggedInUserFull(){
        var user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not logged in");
        }
        var userOpt = userRepository.findById(user.getId());
        if(userOpt.isEmpty()){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return userOpt.get();
    }
}
