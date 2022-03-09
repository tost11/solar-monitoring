package de.tostsoft.solarmonitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.tostsoft.solarmonitoring.JwtUtil;
import de.tostsoft.solarmonitoring.dtos.UserDTO;
import de.tostsoft.solarmonitoring.dtos.UserLoginDTO;
import de.tostsoft.solarmonitoring.dtos.UserRegisterDTO;
import de.tostsoft.solarmonitoring.dtos.admin.UpdateUserForAdminDTO;
import de.tostsoft.solarmonitoring.dtos.admin.UserForAdminDTO;
import de.tostsoft.solarmonitoring.dtos.admin.UserTableRowForAdminDTO;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import de.tostsoft.solarmonitoring.utils.NumberComparator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.driver.Driver;
import org.neo4j.driver.internal.InternalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    @Autowired
    private Driver driver;

    private ObjectMapper neo4jObjectMapper = new ObjectMapper();

    @PostConstruct
    void initUserConstrain(){
        //TODO check if this here is working
        userRepository.initNameConstrain();

        neo4jObjectMapper.registerModule(new JavaTimeModule());
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
            .numAllowedSystems(0)
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

    public boolean checkUsernameAlreadyTaken(UserRegisterDTO userRegisterDTO) {
        return userRepository.countByNameIgnoreCase(userRegisterDTO.getName()) != 0;
    }

    UserForAdminDTO convertUserToUserForAdminDTO(User user){
        return UserForAdminDTO.builder()
            .id(user.getId())
            .isAdmin(user.isAdmin())
            .name(user.getName())
            .numbAllowedSystems(user.getNumAllowedSystems())
            .creationDate(Date.from(user.getCreationDate()))
            .isDeleted(user.getLabels().contains(""+Neo4jLabels.IS_DELETED))
            .build();
    }

    public UserForAdminDTO editUser(UpdateUserForAdminDTO userDTO){
        User oldUser = userRepository.findById(userDTO.getId());
        if(oldUser == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        var userNode = Cypher.node(""+Neo4jLabels.User).named("u");
        List<Expression> ops = new ArrayList<>();
        if(oldUser.isAdmin() != userDTO.isAdmin()){
            ops.add(userNode.property("isAdmin").to(Cypher.literalOf(userDTO.isAdmin())));
        }
        if(!NumberComparator.compare(oldUser.getNumAllowedSystems(),userDTO.getNumbAllowedSystems())){
            ops.add(userNode.property("numAllowedSystems").to(Cypher.literalOf(userDTO.getNumbAllowedSystems())));
        }

        if(ops.isEmpty()){
            //nothing todo here
            return convertUserToUserForAdminDTO(oldUser);
        }

        var statement = Cypher.match(userNode).where(userNode.internalId().eq(Cypher.literalOf(oldUser.getId()))).set(ops).returning(userNode).build();

        var res = driver.session().writeTransaction(tx->tx.run(statement.getCypher()).single());
        var resultNode = (InternalNode)res.get(0).asObject();

        var resSol = neo4jObjectMapper.convertValue(resultNode.asMap(), User.class);
        resSol.setId(resultNode.id());
        return  convertUserToUserForAdminDTO(resSol);
    }



    public List<UserTableRowForAdminDTO> findUserForAdmin(String name) {
        List<User> userList = userRepository.findAllInitializedAndAdminStartsWith(name);
        List<UserTableRowForAdminDTO> userDTOS = new ArrayList<>();
        for(User user:userList){
            UserTableRowForAdminDTO userDTO= new UserTableRowForAdminDTO(user.getId(),user.getName(),user.getNumAllowedSystems(),user.isAdmin());
            userDTOS.add(userDTO);
        }
        return userDTOS;
    }
}
