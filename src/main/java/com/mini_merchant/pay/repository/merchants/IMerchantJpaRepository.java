package com.mini_merchant.pay.repository.merchants;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mini_merchant.pay.entity.Merchants;

public interface IMerchantJpaRepository extends JpaRepository<Merchants, UUID> {
    List<Merchants> findAllByIsDeletedFalse();
}
