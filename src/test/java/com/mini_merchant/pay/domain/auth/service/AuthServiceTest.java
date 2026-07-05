package com.mini_merchant.pay.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.mini_merchant.pay.common.constant.Role;
import com.mini_merchant.pay.common.exception.ConflictException;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterReqModel;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterResModel;
import com.mini_merchant.pay.entity.Users;
import com.mini_merchant.pay.repository.users.IUserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private IUserRepository iUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterReqModel buildRegisterReq() {
        RegisterReqModel req = new RegisterReqModel();
        req.setEmail("user@example.com");
        req.setPassword("password123");
        return req;
    }

    @Test
    void register_success() {
        when(iUserRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");
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
}
