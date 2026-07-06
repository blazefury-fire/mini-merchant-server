package com.mini_merchant.pay.repository.ledgerentries;

import org.springframework.stereotype.Repository;

import com.mini_merchant.pay.entity.LedgerEntries;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LedgerEntryRepository implements ILedgerEntryRepository {
    private final ILedgerEntryJpaRepository iLedgerEntryJpaRepository;

    @Override
    public LedgerEntries save(LedgerEntries ledgerEntry) {
        return iLedgerEntryJpaRepository.save(ledgerEntry);
    }
}
