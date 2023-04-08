package de.tostsoft.solarmonitoring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.tostsoft.solarmonitoring.dtos.GenericDataDTO;
import de.tostsoft.solarmonitoring.JwtUtil;
import de.tostsoft.solarmonitoring.dtos.admin.UpdateUserForAdminDTO;
import de.tostsoft.solarmonitoring.dtos.admin.UserForAdminDTO;
import de.tostsoft.solarmonitoring.dtos.admin.UserTableRowForAdminDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserLoginDTO;
import de.tostsoft.solarmonitoring.dtos.users.UserRegisterDTO;
import de.tostsoft.solarmonitoring.model.Neo4jLabels;
import de.tostsoft.solarmonitoring.model.Neo4jUser;
import de.tostsoft.solarmonitoring.repository.InfluxConnection;
import de.tostsoft.solarmonitoring.repository.Neo4jUserRepository;
import de.tostsoft.solarmonitoring.utils.NumberComparator;
import jakarta.annotation.PostConstruct;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.driver.Driver;
import org.neo4j.driver.internal.InternalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Neo4jUserRepository neo4jUserRepository;

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private JwtUtil jwtTokenUnit;

    @Autowired
    private InfluxConnection influxConnection;

    @Autowired
    private Driver driver;

    private ObjectMapper neo4jObjectMapper = new ObjectMapper();

    @PostConstruct
    void initUserConstrain() {
        //TODO check if this here is working
        neo4jUserRepository.initNameConstrain();

        neo4jObjectMapper.registerModule(new JavaTimeModule());
    }

    public UserDTO loginUser(UserLoginDTO userLoginDTO) {
        var authentication = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(userLoginDTO.getName(), userLoginDTO.getPassword()));
        var user = (Neo4jUser) authentication.getPrincipal();
        String jwt = jwtTokenUnit.generateToken(user);
        UserDTO userDTO = new UserDTO(user.getId(), userLoginDTO.getName());
        userDTO.setJwt(jwt);
        userDTO.setAdmin(user.getIsAdmin());
        return userDTO;
    }

    public UserDTO registerUser(UserRegisterDTO userRegisterDTO) {
        Set<String> labels = new HashSet<>();
        labels.add(Neo4jLabels.User.toString());

        Neo4jUser neo4jUser = Neo4jUser.builder()
                .name(userRegisterDTO.getName())
                .creationDate(ZonedDateTime.now())
                .numAllowedSystems(0)
                .password(passwordEncoder.encode(userRegisterDTO.getPassword()))
                .isAdmin(false)
                .labels(labels)
                .build();

        neo4jUser = neo4jUserRepository.save(neo4jUser);

        //TODO fix that by using string id
        String generatedName = "user-" + neo4jUser.getId();
        influxConnection.createNewBucket(generatedName);

        LOG.info("Created new user with name: {}", neo4jUser.getName());

        UserDTO userDTO = new UserDTO(neo4jUser.getId(), neo4jUser.getName());
        userDTO.setJwt(jwtTokenUnit.generateToken(neo4jUser));
        return userDTO;
    }

    public boolean checkUsernameAlreadyTaken(UserRegisterDTO userRegisterDTO) {
        return neo4jUserRepository.countByNameIgnoreCase(userRegisterDTO.getName()) != 0;
    }

    UserForAdminDTO convertUserToUserForAdminDTO(Neo4jUser neo4jUser) {
        return UserForAdminDTO.builder()
                .id(neo4jUser.getId())
                .isAdmin(neo4jUser.getIsAdmin())
                .name(neo4jUser.getName())
                .numbAllowedSystems(neo4jUser.getNumAllowedSystems())
                .creationDate(neo4jUser.getCreationDate())
                .isDeleted(neo4jUser.getLabels().contains("" + Neo4jLabels.IS_DELETED))
                .build();
    }

    public UserForAdminDTO editUser(UpdateUserForAdminDTO userDTO) {
        Neo4jUser oldNeo4jUser = neo4jUserRepository.findById(userDTO.getId());
        if (oldNeo4jUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        var userNode = Cypher.node("" + Neo4jLabels.User).named("u");
        List<Expression> ops = new ArrayList<>();
        if (oldNeo4jUser.getIsAdmin() != userDTO.isAdmin()) {
            ops.add(userNode.property("isAdmin").to(Cypher.literalOf(userDTO.isAdmin())));
        }
        if (!NumberComparator.compare(oldNeo4jUser.getNumAllowedSystems(), userDTO.getNumAllowedSystems())) {
            ops.add(userNode.property("numAllowedSystems").to(Cypher.literalOf(userDTO.getNumAllowedSystems())));
        }

        if (ops.isEmpty()) {
            //nothing todo here
            return convertUserToUserForAdminDTO(oldNeo4jUser);
        }

        var statement = Cypher.match(userNode).where(userNode.internalId().eq(Cypher.literalOf(oldNeo4jUser.getId()))).set(ops).returning(userNode).build();

        var session = driver.session();
        var res = session.writeTransaction(tx -> tx.run(statement.getCypher()).single());
        var resultNode = (InternalNode) res.get(0).asObject();

        var resSol = neo4jObjectMapper.convertValue(resultNode.asMap(), Neo4jUser.class);
        resSol.setId(resultNode.id());
        resSol.setLabels(new HashSet<>(resultNode.labels()));
        return convertUserToUserForAdminDTO(resSol);
    }

    public List<UserTableRowForAdminDTO> findUserForAdmin(String name) {
        //or ony exist users
        List<Neo4jUser> neo4jUserList = neo4jUserRepository.findAllInitializedAndAdminStartsWith(name);
        List<UserTableRowForAdminDTO> userDTOS = new ArrayList<>();
        for(Neo4jUser neo4jUser : neo4jUserList){
            UserTableRowForAdminDTO userDTO = new UserTableRowForAdminDTO(neo4jUser.getId(), neo4jUser.getName(), neo4jUser.getNumAllowedSystems(), neo4jUser.getIsAdmin(), false);
            if(neo4jUser.getLabels().contains(Neo4jLabels.IS_DELETED.toString()))
                userDTO = new UserTableRowForAdminDTO(neo4jUser.getId(), neo4jUser.getName(), neo4jUser.getNumAllowedSystems(), neo4jUser.getIsAdmin(), true);

            userDTOS.add(userDTO);
        }
        return userDTOS;
    }

    public List<GenericDataDTO> findUsers(String name) {
        List<Neo4jUser> neo4jUserList = neo4jUserRepository.findAllInitializedAndAdminStartsWith(name);
        return neo4jUserList.stream().map(u->new GenericDataDTO(u.getId(),u.getName())).collect(Collectors.toList());
    }

    public boolean isUserFromContextAdmin(){
        Neo4jUser neo4jUser = (Neo4jUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return neo4jUserRepository.isUserAdmin(neo4jUser.getId());
    }
}
