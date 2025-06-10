package com.melodical.backend.service;

import com.melodical.backend.dto.AuthResponse;
import com.melodical.backend.dto.LoginRequest;
import com.melodical.backend.dto.SignupRequest;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.UserRepository;
import com.melodical.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(SignupRequest req) {
        userRepository.findByEmail(req.getEmail())
                .ifPresent(u -> { throw new RuntimeException("이미 사용중인 이메일입니다."); });

        String encoded = passwordEncoder.encode(req.getPassword());
        User user = userRepository.save(User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role("ROLE_USER")
                .provider(null)
                .build()
        );

        String token = jwtService.generateToken(user.getEmail(), user.getId());
        return new AuthResponse(token, user.getEmail(), user.getId());
    }

    public AuthResponse authenticate(LoginRequest req) {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("사용자 없음"));
        String token = jwtService.generateToken(user.getEmail(), user.getId());
        return new AuthResponse(token, user.getEmail(), user.getId());
    }
}