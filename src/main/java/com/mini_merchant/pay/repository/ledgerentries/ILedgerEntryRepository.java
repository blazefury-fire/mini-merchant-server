package com.mini_merchant.pay.repository.ledgerentries;

import com.mini_merchant.pay.entity.LedgerEntries;

public interface ILedgerEntryRepository {
    LedgerEntries save(LedgerEntries ledgerEntry);
}
