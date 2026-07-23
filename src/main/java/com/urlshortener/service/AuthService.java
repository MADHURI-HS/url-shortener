package com.urlshortener.service;

import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.LoginResponse;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.exception.UserAlreadyExistsException;
import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.urlshortener.exception.InvalidCredentialsException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(RegisterRequest request) {

        Optional<User> existingUser =
                userRepository.findByEmail(request.getEmail());

        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException(
                    "Email is already registered");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole("ROLE_USER");   // ✅ This is the important line

        userRepository.save(user);
    }
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new InvalidCredentialsException(
                    "Invalid email or password");
        }
        String token = jwtService.generateToken(user);


        return new LoginResponse(token);
    }
}
