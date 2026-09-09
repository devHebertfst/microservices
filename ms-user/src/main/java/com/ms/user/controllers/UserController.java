package com.ms.user.controllers;

import com.ms.user.dtos.LoginRequestDto;
import com.ms.user.dtos.LoginResponseDto;
import com.ms.user.dtos.UserRecordDto;
import com.ms.user.models.User;
import com.ms.user.repositories.UserRepository;
import com.ms.user.security.TokenService;
import com.ms.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class UserController {

    final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    public UserController(UserService userService, UserRepository userRepository, PasswordEncoder passwordEncoder,
                          TokenService tokenService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> saveUser(@RequestBody @Valid UserRecordDto userRecordDto){
        if(userRepository.existsByEmail(userRecordDto.email())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already registered"));
        }
        var userModel = new User();
        BeanUtils.copyProperties(userRecordDto, userModel);
        userModel.setPassword(passwordEncoder.encode(userRecordDto.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.save(userModel));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDto loginRequestDto){
        var usernamePassword = new UsernamePasswordAuthenticationToken(loginRequestDto.email(), loginRequestDto.password());
        var auth = authenticationManager.authenticate(usernamePassword);
        var user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        var token = tokenService.generateToken(user);
        return ResponseEntity.ok(new LoginResponseDto(token));
    }
}
