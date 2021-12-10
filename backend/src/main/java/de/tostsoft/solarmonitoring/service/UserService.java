package de.tostsoft.solarmonitoring.service;

import de.tostsoft.solarmonitoring.JwtUtil;
import de.tostsoft.solarmonitoring.dtos.UserLoginDTO;
import de.tostsoft.solarmonitoring.exception.ApiRequestException;
import de.tostsoft.solarmonitoring.exception.InternalServerException;
import de.tostsoft.solarmonitoring.exception.NotFoundException;
import de.tostsoft.solarmonitoring.exception.UnAuthorizedError;
import de.tostsoft.solarmonitoring.model.User;
import de.tostsoft.solarmonitoring.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private void registeCheck(User user) {
        user.setPassword(StringUtils.trim(user.getPassword()));
        user.setName(StringUtils.trim(user.getName()));
        LOG.info("Trim Password and UserName");
    }


    /**
     * @param userLoginDTO
     * @return
     * @throws Exception
     */
    public ResponseEntity loginMachCheck(UserLoginDTO userLoginDTO) {
        var neo4jUser = userReposetory.findByNameIgnoreCase(userLoginDTO.getName());
        if (!StringUtils.equals(userLoginDTO.getName(), neo4jUser.getName())) {
            throw new ApiRequestException("User Not Exist");
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userLoginDTO.getName(), userLoginDTO.getPassword()));
            final User user = loadUserByUsername(userLoginDTO.getName());
            final String jwt = jwtTokenUnit.generateToken(user);
            return ResponseEntity.status(HttpStatus.OK).body(jwt);
        } catch (Exception e) {

        }
        throw new UnAuthorizedError("User and Password not match");
    }

    public ResponseEntity userRegister(UserLoginDTO userLoginDTO) {
        try {
            User user = new User(userLoginDTO.getName(), userLoginDTO.getPassword());
            registeCheck(user);
            user.setPassword(passwordEncoder.encode(userLoginDTO.getPassword()));
            user = userReposetory.save(user);
            System.out.println(user);
            final String jwt = jwtTokenUnit.generateToken(user);
            return ResponseEntity.status(HttpStatus.OK).body(jwt);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalServerException("User can't save");
        }


    }

    public boolean isUserAlreadyExists(UserLoginDTO userLoginDTO) {
        userReposetory.countByNameIgnoreCase(userLoginDTO.getName());
        return userReposetory.countByNameIgnoreCase(userLoginDTO.getName()) != 0;
    }

    public User getUserByName(String name) {
        // User user = new User();
        // userRepository.update(getUserByName(name), user);
        return userReposetory.findByNameIgnoreCase(name);
    }

    @Override
    public User loadUserByUsername(String name) throws NotFoundException {
        return userReposetory.findByNameIgnoreCase(name);
    }
}
