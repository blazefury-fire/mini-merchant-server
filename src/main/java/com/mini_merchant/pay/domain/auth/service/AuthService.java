package com.mini_merchant.pay.domain.auth.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mini_merchant.pay.common.constant.Role;
import com.mini_merchant.pay.common.exception.ConflictException;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterReqModel;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterResModel;
import com.mini_merchant.pay.entity.Users;
import com.mini_merchant.pay.repository.users.IUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    private final IUserRepository iUserRepository;
    private final PasswordEncoder passwordEncoder;

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
}
