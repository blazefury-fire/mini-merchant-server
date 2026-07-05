package com.mini_merchant.pay.repository.users;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.mini_merchant.pay.entity.Users;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepository implements IUserRepository {
    private final IUserJpaRepository iUserJpaRepository;

    @Override
    public Users save(Users user) {
        return iUserJpaRepository.save(user);
    }

    @Override
    public Optional<Users> findByEmail(String email) {
        return iUserJpaRepository.findByEmailAndIsDeletedFalse(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return iUserJpaRepository.existsByEmailAndIsDeletedFalse(email);
    }

    @Override
    public Optional<Users> findById(UUID id) {
        return iUserJpaRepository.findByIdAndIsDeletedFalse(id);
    }
}
