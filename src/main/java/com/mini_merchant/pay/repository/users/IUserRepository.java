package com.mini_merchant.pay.repository.users;

import java.util.Optional;

import com.mini_merchant.pay.entity.Users;

public interface IUserRepository {
    Users save(Users user);

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);
}
