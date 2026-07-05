package com.mini_merchant.pay.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private IUserRepository iUserRepository;

    @Mock
    private IRefreshTokenRepository iRefreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private RegisterReqModel buildRegisterReq() {
        RegisterReqModel req = new RegisterReqModel();
        req.setEmail("user@example.com");
        req.setPassword("Passw0rd!");
        return req;
    }

    private LoginReqModel buildLoginReq() {
        LoginReqModel req = new LoginReqModel();
        req.setEmail("user@example.com");
        req.setPassword("Passw0rd!");
        return req;
    }

    private RefreshReqModel buildRefreshReq() {
        RefreshReqModel req = new RefreshReqModel();
        req.setRefreshToken("refresh-token-value");
        return req;
    }

    private LogoutReqModel buildLogoutReq() {
        LogoutReqModel req = new LogoutReqModel();
        req.setRefreshToken("refresh-token-value");
        return req;
    }

    private Users buildUser() {
        Users user = new Users();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed-pw");
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setCreatedBy("user@example.com");
        user.setIsDeleted(false);
        return user;
    }

    private RefreshTokens buildStoredToken(UUID userId, LocalDateTime expiresAt) {
        RefreshTokens token = new RefreshTokens();
        token.setId(UUID.randomUUID());
        token.setUserId(userId);
        token.setToken("refresh-token-value");
        token.setExpiresAt(expiresAt);
        token.setCreatedAt(LocalDateTime.now());
        token.setCreatedBy("user@example.com");
        token.setIsDeleted(false);
        return token;
    }

    @Test
    void register_success() {
        when(iUserRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("hashed-pw");
        when(iUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterResModel result = authService.register(buildRegisterReq());

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(iUserRepository, times(1)).save(captor.capture());

        Users saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-pw");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getIsDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("user@example.com");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(iUserRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRegisterReq()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("user@example.com");

        verify(iUserRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void login_success() {
        Users user = buildUser();
        when(iUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Passw0rd!", "hashed-pw")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access.jwt.token");
        when(jwtTokenProvider.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpiry()).thenReturn(LocalDateTime.now().plusDays(7));
        when(iRefreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginResModel result = authService.login(buildLoginReq());

        assertThat(result.getAccessToken()).isEqualTo("access.jwt.token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(900L);
        assertThat(result.getRefreshToken()).isNotBlank();

        ArgumentCaptor<RefreshTokens> captor = ArgumentCaptor.forClass(RefreshTokens.class);
        verify(iRefreshTokenRepository, times(1)).save(captor.capture());

        RefreshTokens saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
        assertThat(saved.getToken()).isEqualTo(result.getRefreshToken());
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(saved.getIsDeleted()).isFalse();
        assertThat(saved.getCreatedBy()).isEqualTo("user@example.com");
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        when(iUserRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(buildLoginReq()))
                .isInstanceOf(UnauthorizedException.class);

        verify(iRefreshTokenRepository, never()).save(any());
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        Users user = buildUser();
        when(iUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Passw0rd!", "hashed-pw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(buildLoginReq()))
                .isInstanceOf(UnauthorizedException.class);

        verify(iRefreshTokenRepository, never()).save(any());
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }

    @Test
    void refresh_success() {
        Users user = buildUser();
        RefreshTokens stored = buildStoredToken(user.getId(), LocalDateTime.now().plusDays(3));
        when(iRefreshTokenRepository.findByToken("refresh-token-value")).thenReturn(Optional.of(stored));
        when(iUserRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new.access.token");
        when(jwtTokenProvider.getAccessTokenTtlSeconds()).thenReturn(900L);

        LoginResModel result = authService.refresh(buildRefreshReq());

        assertThat(result.getAccessToken()).isEqualTo("new.access.token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token-value");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(900L);

        verify(iRefreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_invalidToken_throwsUnauthorized() {
        when(iRefreshTokenRepository.findByToken("refresh-token-value")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(buildRefreshReq()))
                .isInstanceOf(UnauthorizedException.class);

        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }

    @Test
    void refresh_expiredToken_throwsUnauthorized() {
        RefreshTokens stored = buildStoredToken(UUID.randomUUID(), LocalDateTime.now().minusMinutes(1));
        when(iRefreshTokenRepository.findByToken("refresh-token-value")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(buildRefreshReq()))
                .isInstanceOf(UnauthorizedException.class);

        verify(iUserRepository, never()).findById(any());
        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }

    @Test
    void refresh_userNotFound_throwsUnauthorized() {
        RefreshTokens stored = buildStoredToken(UUID.randomUUID(), LocalDateTime.now().plusDays(3));
        when(iRefreshTokenRepository.findByToken("refresh-token-value")).thenReturn(Optional.of(stored));
        when(iUserRepository.findById(stored.getUserId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(buildRefreshReq()))
                .isInstanceOf(UnauthorizedException.class);

        verify(jwtTokenProvider, never()).generateAccessToken(any());
    }

    @Test
    void logout_existingToken_softDeletesIt() {
        RefreshTokens stored = buildStoredToken(UUID.randomUUID(), LocalDateTime.now().plusDays(3));
        when(iRefreshTokenRepository.findByToken("refresh-token-value")).thenReturn(Optional.of(stored));
        when(iRefreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.logout(buildLogoutReq());

        ArgumentCaptor<RefreshTokens> captor = ArgumentCaptor.forClass(RefreshTokens.class);
        verify(iRefreshTokenRepository, times(1)).save(captor.capture());

        RefreshTokens saved = captor.getValue();
        assertThat(saved.getIsDeleted()).isTrue();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void logout_unknownToken_isIdempotent() {
        when(iRefreshTokenRepository.findByToken("refresh-token-value")).thenReturn(Optional.empty());

        authService.logout(buildLogoutReq());

        verify(iRefreshTokenRepository, never()).save(any());
    }
}
