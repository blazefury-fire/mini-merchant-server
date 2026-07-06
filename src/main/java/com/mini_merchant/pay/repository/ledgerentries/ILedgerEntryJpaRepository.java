package com.mini_merchant.pay.repository.ledgerentries;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mini_merchant.pay.entity.LedgerEntries;

public interface ILedgerEntryJpaRepository extends JpaRepository<LedgerEntries, UUID> {
}
