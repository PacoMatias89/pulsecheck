package com.pulsecheck.service;

import com.pulsecheck.domain.entity.User;
import com.pulsecheck.domain.entity.Workspace;
import com.pulsecheck.dto.request.LoginRequest;
import com.pulsecheck.dto.request.RegisterRequest;
import com.pulsecheck.dto.response.AuthResponse;
import com.pulsecheck.dto.response.UserResponse;
import com.pulsecheck.exception.BusinessException;
import com.pulsecheck.repository.UserRepository;
import com.pulsecheck.repository.WorkspaceRepository;
import com.pulsecheck.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException("Email already registered");
        }

        var user = User.builder()
                .email(req.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName().trim())
                .apiKey(generateApiKey())
                .build();
        userRepository.save(user);

        var slug = generateWorkspaceSlug(req.fullName());
        var workspace = Workspace.builder()
                .owner(user)
                .name(req.fullName() + "'s Workspace")
                .slug(slug)
                .build();
        workspaceRepository.save(workspace);

        var token = tokenProvider.generateToken(user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse login(LoginRequest req) {
        var user = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        var token = tokenProvider.generateToken(user.getEmail());
        return new AuthResponse(token, UserResponse.from(user));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private String generateApiKey() {
        var bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return "pk_" + HexFormat.of().formatHex(bytes);
    }

    private String generateWorkspaceSlug(String fullName) {
        var base = fullName.toLowerCase().replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "");
        var slug = base;
        int counter = 1;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
