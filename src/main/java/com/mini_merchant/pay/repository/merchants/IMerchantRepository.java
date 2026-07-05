package com.mini_merchant.pay.repository.merchants;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mini_merchant.pay.entity.Merchants;

public interface IMerchantRepository {
    Merchants save(Merchants merchant);

    Optional<Merchants> findById(UUID id);

    List<Merchants> findAllActive();

    Optional<Merchants> findByApiKey(String apiKey);
}
