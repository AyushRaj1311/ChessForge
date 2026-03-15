package com.chessplatform.service;

import com.chessplatform.dto.request.LoginRequest;
import com.chessplatform.dto.request.RegisterRequest;
import com.chessplatform.dto.response.AuthResponse;
import com.chessplatform.exception.AuthException;
import com.chessplatform.model.User;
import com.chessplatform.repository.UserRepository;
import com.chessplatform.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();
        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser);
        log.info("Registered new user: {}", savedUser.getUsername());

        return AuthResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .userId(savedUser.getId())
            .username(savedUser.getUsername())
            .role(savedUser.getRole())
            .rating(savedUser.getRating())
            .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            if (!(auth.getPrincipal() instanceof User user)) {
                throw new AuthException("Authentication failed: invalid principal");
            }
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            String token = jwtUtil.generateToken(user);
            log.info("User logged in: {}", user.getUsername());

            return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .rating(user.getRating())
                .build();

        } catch (BadCredentialsException e) {
            throw new AuthException("Invalid username or password");
        }
    }
}
