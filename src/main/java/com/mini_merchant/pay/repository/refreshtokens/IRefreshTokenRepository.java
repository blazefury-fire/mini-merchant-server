package com.mini_merchant.pay.repository.refreshtokens;

import java.util.Optional;

import com.mini_merchant.pay.entity.RefreshTokens;

public interface IRefreshTokenRepository {
    RefreshTokens save(RefreshTokens refreshToken);

    Optional<RefreshTokens> findByToken(String token);
}
