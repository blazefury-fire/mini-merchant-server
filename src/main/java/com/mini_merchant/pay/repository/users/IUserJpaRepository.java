package com.mini_merchant.pay.repository.users;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mini_merchant.pay.entity.Users;

public interface IUserJpaRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);

    Optional<Users> findByIdAndIsDeletedFalse(UUID id);
}
