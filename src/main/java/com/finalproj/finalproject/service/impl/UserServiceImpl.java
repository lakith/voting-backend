package com.finalproj.finalproject.service.impl;

import com.finalproj.finalproject.configuration.ApiParameters;
import com.finalproj.finalproject.dto.*;
import com.finalproj.finalproject.jwt.JwtGenerator;
import com.finalproj.finalproject.model.*;
import com.finalproj.finalproject.repository.CompetitorsRepository;
import com.finalproj.finalproject.repository.DepartmentRepository;
import com.finalproj.finalproject.repository.UserRepository;
import com.finalproj.finalproject.repository.UserRoleRepository;
import com.finalproj.finalproject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private final BCryptPasswordEncoder bCryptPasswordEncoder ;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CompetitorsRepository competitorsRepository;

    public UserServiceImpl(BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public ResponseEntity<?> saveNewUser(UserDTO userDTO) throws Exception {
        ResponseModel responseModel = new ResponseModel();
        if(userRepository.getUserByUsernameForSignUp(userDTO.getUsername()).isPresent()){
            responseModel.setMessage("Username Already Exists!");
            responseModel.setStatus(false);
            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
        }
        if(userRepository.getUserByEmail(userDTO.getEmail()).isPresent()){
            responseModel.setMessage("Email Already Exists!");
            responseModel.setStatus(false);
            return new ResponseEntity<>(responseModel,HttpStatus.BAD_REQUEST);
        }

        Optional<UserRole> optionalUserRole = userRoleRepository.getRoleByRoleType(userDTO.getRoleType());

        if(!optionalUserRole.isPresent()){
            responseModel.setMessage("Invalid User Role");
            responseModel.setStatus(false);
            return new ResponseEntity<>(responseModel,HttpStatus.BAD_REQUEST);
        }

        String encryptedPassword = bCryptPasswordEncoder.encode(userDTO.getPassword());

        User newUser = new User();
        newUser.setUsername(userDTO.getUsername());
        newUser.setName(userDTO.getName());
        newUser.setEmail(userDTO.getEmail());
        newUser.setUserRole(optionalUserRole.get());
        newUser.setPassword(encryptedPassword);
        newUser.setActive(true);

        try {
            userRepository.save(newUser);
            responseModel.setMessage("User Added Successfully!");
            responseModel.setStatus(false);
            return new ResponseEntity<>(responseModel,HttpStatus.CREATED);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    @Override
    public  ResponseEntity<?> saveCompetitor(CompetitorDTO userDTO) throws Exception {

        ResponseModel responseModel = new ResponseModel();
        if(userRepository.getUserByUsernameForSignUp(userDTO.getUsername()).isPresent()){
            responseModel.setMessage("Username Already Exists.!");
            responseModel.setStatus(false);
            return new ResponseEntity<>(responseModel, HttpStatus.BAD_REQUEST);
        }
        if(userRepository.getUserByEmail(userDTO.getEmail()).isPresent()){
            responseModel.setMessage("Email Already Exists.!");
            responseModel.setStatus(false);
            return new ResponseEntity<>(responseModel,HttpStatus.BAD_REQUEST);
        }

        Optional<UserRole> optionalUserRole = userRoleRepository.getRoleByRoleType(userDTO.getRoleType());

        if(!optionalUserRole.isPresent()){
            responseModel.setMessage("Invalid User Role.");
            responseModel.setStatus(false);
            return new ResponseEntity<>(responseModel,HttpStatus.BAD_REQUEST);
        }

        String encryptedPassword = bCryptPasswordEncoder.encode(userDTO.getPassword());

        User newUser = new User();
        newUser.setUsername(userDTO.getUsername());
        newUser.setName(userDTO.getName());
        newUser.setEmail(userDTO.getEmail());
        newUser.setUserRole(optionalUserRole.get());
        newUser.setPassword(encryptedPassword);
        newUser.setActive(true);

        try {
            newUser = userRepository.save(newUser);

            try {

                Optional<Department> department = departmentRepository.findById(userDTO.getDepartmentId());
                if(!department.isPresent()){
                    return new ResponseEntity<>(new ResponseModel("Invalied department id",false),HttpStatus.BAD_REQUEST);
                }
                Competitors competitors = new Competitors();
                competitors.setUser(newUser);
                competitors.setDepartment(department.get());
                competitors.setVotes(0);

                competitorsRepository.save(competitors);

            } catch (Exception e){
                throw new Exception(e.getMessage());
            }

            responseModel.setMessage("User Added Successfully!.");
            responseModel.setStatus(false);
            return new ResponseEntity<>(responseModel,HttpStatus.CREATED);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    @Override
    public  ResponseEntity<?> saveVote(VoteDTO voteDTO, Principal principal){

        Optional<User> optionalUser = userRepository.findById(Integer.parseInt(principal.getName()));
        if(!optionalUser.isPresent()){
            return new ResponseEntity<>(new ResponseModel("Invalid attempt",false),HttpStatus.BAD_REQUEST);
        }
        User user = optionalUser.get();
        if(user.isVoted()){
            return new ResponseEntity<>(new ResponseModel("You Have Already Voted",false),HttpStatus.BAD_REQUEST);
        }
//        Optional<Competitors> optionalCompetitors = competitorsRepository.getCompetitorsByUserId(voteDTO.getCompetitorId());
        Optional<Competitors> optionalCompetitors = competitorsRepository.findById(voteDTO.getCompetitorId());
        if(!optionalCompetitors.isPresent()){
            return new ResponseEntity<>(new ResponseModel("Invalid attempt",false),HttpStatus.BAD_REQUEST);
        }

        try {
            user.setVoted(true);
            userRepository.save(user);

            Competitors competitors = optionalCompetitors.get();
            int votes = competitors.getVotes();
            votes += 1;
            competitors.setVotes(votes);
            competitorsRepository.save(competitors);

            return new ResponseEntity<>(new ResponseModel("Vote Added Successfully",true),HttpStatus.OK);
        } catch (Exception e){
            return new ResponseEntity<>(new ResponseModel("Something went Wrong",false),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> getAllUsers(){
        List<User> userList = userRepository.findAll();
        if(userList.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }   else {
            return new ResponseEntity<>(userList,HttpStatus.OK);
        }
    }

    @Override
    public ResponseEntity<?> userLogin(LoginDTO loginDTO) {
        ResponseModel responseModel = new ResponseModel();
        Optional<User> optionalUser = userRepository.getUserByUsername(loginDTO.getUsername());
        if(!optionalUser.isPresent()){
            responseModel.setMessage("Invalid Login Credentials OR User Is Not Active");
            responseModel.setStatus(false);
            return new ResponseEntity<>(responseModel,HttpStatus.UNAUTHORIZED);
        }

        if(bCryptPasswordEncoder.matches(loginDTO.getPassword(), optionalUser.get().getPassword())) {
            String accessToken = createJWtWithOutPrefix(optionalUser.get());
            if (accessToken == null) {
                responseModel.setMessage("Something went Wrong");
                responseModel.setStatus(false);
                return new ResponseEntity<>(responseModel, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            String refreshToken = createRefreshToken(optionalUser.get());
            if (refreshToken == null) {
                responseModel.setMessage("Something went Wrong");
                responseModel.setStatus(false);
                return new ResponseEntity<>(responseModel, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            AuthToken authToken = new AuthToken();
            authToken.setAccessToken(accessToken);
            authToken.setRefreshToken(refreshToken);

            DisplayUserDTO displayUserDTO = new DisplayUserDTO();
            displayUserDTO.setAuthToken(authToken);
            displayUserDTO.setEmail(optionalUser.get().getEmail());
            displayUserDTO.setUserId(optionalUser.get().getUserId());
            displayUserDTO.setName(optionalUser.get().getName());
            displayUserDTO.setUserName(optionalUser.get().getUsername());

            return new ResponseEntity<>(displayUserDTO, HttpStatus.OK);
            } else {
                responseModel.setMessage("Invalid Password Entered");
                responseModel.setStatus(false);
                return new ResponseEntity<>(responseModel,HttpStatus.UNAUTHORIZED);
            }




    }

    @Override
    public ResponseEntity<?> activateAUser(int userId){
        Optional<User> optionalUser = userRepository.findById(userId);
        if(!optionalUser.isPresent()){
            return new ResponseEntity<>(new ResponseModel("Invalid User Id",false),HttpStatus.BAD_REQUEST);
        } else{
            User user = optionalUser.get();
            user.setActive(true);
            userRepository.save(user);
            return new ResponseEntity<>(new ResponseModel("User Activated successfully",true),HttpStatus.OK);
        }
    }


    private String createJWtWithOutPrefix(User user) {
        List<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_"+user.getUserRole().getRoleType()));
        String accessToken = null;
        try {
            accessToken = JwtGenerator.genarateAccessJWT(user.getUsername(),Integer.toString(user.getUserId()), grantedAuthorities, ApiParameters.JWT_EXPIRATION, ApiParameters.JWT_SECRET);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return accessToken;
    }

    private String createRefreshToken(User user) {
        List<SimpleGrantedAuthority> grantedAuthorityList = new ArrayList<>();
        grantedAuthorityList.add(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().getRoleType()));
        String refreshToken = JwtGenerator.generateRefreshToken(user.getUsername(),Integer.toString(user.getUserId()) ,grantedAuthorityList,ApiParameters.REFRESH_TOKEN_EXPIRATION,ApiParameters.JWT_SECRET);
        try {
            int i = userRepository.updateRefreshToken(user.getUsername(), refreshToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return refreshToken;
    }

}
