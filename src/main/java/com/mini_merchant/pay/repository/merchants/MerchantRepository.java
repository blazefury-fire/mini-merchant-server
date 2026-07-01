package com.mini_merchant.pay.repository.merchants;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.mini_merchant.pay.entity.Merchants;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MerchantRepository implements IMerchantRepository {
    private final IMerchantJpaRepository iMerchantJpaRepository;

    @Override
    public Merchants save(Merchants merchant) {
        return iMerchantJpaRepository.save(merchant);
    }

    @Override
    public Optional<Merchants> findById(UUID id) {
        return iMerchantJpaRepository.findById(id);
    }

    @Override
    public List<Merchants> findAllActive() {
        return iMerchantJpaRepository.findAllByIsDeletedFalse();
    }
}
