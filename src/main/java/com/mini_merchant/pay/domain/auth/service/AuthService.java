package com.mini_merchant.pay.domain.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mini_merchant.pay.common.constant.Role;
import com.mini_merchant.pay.common.exception.ConflictException;
import com.mini_merchant.pay.common.exception.UnauthorizedException;
import com.mini_merchant.pay.common.security.JwtTokenProvider;
import com.mini_merchant.pay.domain.auth.dto.login.LoginReqModel;
import com.mini_merchant.pay.domain.auth.dto.login.LoginResModel;
import com.mini_merchant.pay.domain.auth.dto.logout.LogoutReqModel;
import com.mini_merchant.pay.domain.auth.dto.refresh.RefreshReqModel;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterReqModel;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterResModel;
import com.mini_merchant.pay.entity.RefreshTokens;
import com.mini_merchant.pay.entity.Users;
import com.mini_merchant.pay.repository.refreshtokens.IRefreshTokenRepository;
import com.mini_merchant.pay.repository.users.IUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    private final IUserRepository iUserRepository;
    private final IRefreshTokenRepository iRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public RegisterResModel register(RegisterReqModel request) {
        if (iUserRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        Users user = new Users();

        UUID id = UUID.randomUUID();
        user.setId(id);
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy(request.getEmail());
        user.setIsDeleted(false);

        iUserRepository.save(user);

        return RegisterResModel.builder()
                .id(id)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public LoginResModel login(LoginReqModel request) {
        Users user = iUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = createAndStoreRefreshToken(user);

        return LoginResModel.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenTtlSeconds())
                .build();
    }

    public LoginResModel refresh(RefreshReqModel request) {
        RefreshTokens stored = iRefreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        Users user = iUserRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        String accessToken = jwtTokenProvider.generateAccessToken(user);

        return LoginResModel.builder()
                .accessToken(accessToken)
                .refreshToken(stored.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenTtlSeconds())
                .build();
    }

    @Transactional
    public void logout(LogoutReqModel request) {
        iRefreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(stored -> {
                    stored.setIsDeleted(true);
                    stored.setUpdatedAt(LocalDateTime.now());
                    iRefreshTokenRepository.save(stored);
                });
    }

    private String createAndStoreRefreshToken(Users user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshTokens refreshToken = new RefreshTokens();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(jwtTokenProvider.getRefreshTokenExpiry());
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setCreatedBy(user.getEmail());
        refreshToken.setIsDeleted(false);

        iRefreshTokenRepository.save(refreshToken);

        return token;
    }
}
