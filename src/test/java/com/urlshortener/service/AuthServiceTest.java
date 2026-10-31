package com.urlshortener.service;

import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.LoginResponse;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.exception.InvalidCredentialsException;
import com.urlshortener.exception.UserAlreadyExistsException;
import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setName("Jane");
        r.setEmail("jane@example.com");
        r.setPassword("secret");
        return r;
    }

    private LoginRequest loginRequest(String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail("jane@example.com");
        r.setPassword(password);
        return r;
    }

    @Test
    void register_savesUserWithEncodedPasswordAndUserRole() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded");

        authService.register(registerRequest());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("Jane", saved.getValue().getName());
        assertEquals("jane@example.com", saved.getValue().getEmail());
        assertEquals("encoded", saved.getValue().getPassword());
        assertEquals("ROLE_USER", saved.getValue().getRole());
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.findByEmail("jane@example.com"))
                .thenReturn(Optional.of(new User()));

        assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(registerRequest()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsJwtForValidCredentials() {
        User user = User.builder().email("jane@example.com").password("hash").role("ROLE_USER").build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginResponse response = authService.login(loginRequest("secret"));

        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = User.builder().email("jane@example.com").password("hash").role("ROLE_USER").build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(loginRequest("wrong")));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_rejectsUnknownEmail() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(loginRequest("secret")));
    }
}
