package com.mini_merchant.pay.repository.refreshtokens;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mini_merchant.pay.entity.RefreshTokens;

public interface IRefreshTokenJpaRepository extends JpaRepository<RefreshTokens, UUID> {
    Optional<RefreshTokens> findByTokenAndIsDeletedFalse(String token);
}
