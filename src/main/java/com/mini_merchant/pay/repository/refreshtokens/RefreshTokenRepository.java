package com.mini_merchant.pay.repository.refreshtokens;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.mini_merchant.pay.entity.RefreshTokens;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository implements IRefreshTokenRepository {
    private final IRefreshTokenJpaRepository iRefreshTokenJpaRepository;

    @Override
    public RefreshTokens save(RefreshTokens refreshToken) {
        return iRefreshTokenJpaRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshTokens> findByToken(String token) {
        return iRefreshTokenJpaRepository.findByTokenAndIsDeletedFalse(token);
    }
}
