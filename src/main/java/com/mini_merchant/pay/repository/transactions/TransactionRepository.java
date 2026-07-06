package com.mini_merchant.pay.repository.transactions;

import org.springframework.stereotype.Repository;

import com.mini_merchant.pay.entity.Transactions;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TransactionRepository implements ITransactionRepository {
    private final ITransactionJpaRepository iTransactionJpaRepository;

    @Override
    public Transactions save(Transactions transaction) {
        return iTransactionJpaRepository.save(transaction);
    }
}
