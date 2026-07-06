package com.mini_merchant.pay.repository.transactions;

import com.mini_merchant.pay.entity.Transactions;

public interface ITransactionRepository {
    Transactions save(Transactions transaction);
}
