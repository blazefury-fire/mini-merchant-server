package com.mini_merchant.pay.repository.transactions;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mini_merchant.pay.entity.Transactions;

public interface ITransactionJpaRepository extends JpaRepository<Transactions, UUID> {
}
