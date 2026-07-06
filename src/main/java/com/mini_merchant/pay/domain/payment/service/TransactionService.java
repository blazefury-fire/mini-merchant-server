package com.mini_merchant.pay.domain.payment.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.mini_merchant.pay.common.constant.Direction;
import com.mini_merchant.pay.common.constant.TransactionStatus;
import com.mini_merchant.pay.domain.payment.dto.transaction.CreateTransactionReqModel;
import com.mini_merchant.pay.domain.payment.dto.transaction.CreateTransactionResModel;
import com.mini_merchant.pay.entity.LedgerEntries;
import com.mini_merchant.pay.entity.Transactions;
import com.mini_merchant.pay.repository.ledgerentries.ILedgerEntryRepository;
import com.mini_merchant.pay.repository.transactions.ITransactionRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService implements ITransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private static final String PLATFORM_ACCOUNT = "PLATFORM";
    private static final String MERCHANT_ACCOUNT_PREFIX = "MERCHANT:";
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:transaction:";

    private final ITransactionRepository iTransactionRepository;
    private final ILedgerEntryRepository iLedgerEntryRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.idempotency.ttl-hours:24}")
    private long idempotencyTtlHours;

    @Override
    @Transactional
    public CreateTransactionResModel createTransaction(UUID merchantId, String idempotencyKey,
            CreateTransactionReqModel request) {
        String cacheKey = IDEMPOTENCY_KEY_PREFIX + merchantId + ":" + idempotencyKey;

        CreateTransactionResModel cached = readCachedResult(cacheKey);
        if (cached != null) {
            return cached;
        }

        CreateTransactionResModel result = persistTransaction(merchantId, idempotencyKey, request);
        scheduleCacheWrite(cacheKey, result);
        return result;
    }

    private CreateTransactionResModel persistTransaction(UUID merchantId, String idempotencyKey,
            CreateTransactionReqModel request) {
        LocalDateTime now = LocalDateTime.now();
        String actor = merchantId.toString();

        Transactions transaction = new Transactions();
        UUID transactionId = UUID.randomUUID();
        transaction.setId(transactionId);
        transaction.setMerchantId(merchantId);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus(TransactionStatus.PENDING.name());
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setCreatedAt(now);
        transaction.setCreatedBy(actor);
        transaction.setIsDeleted(false);
        iTransactionRepository.save(transaction);

        iLedgerEntryRepository.save(buildLedgerEntry(transactionId,
                MERCHANT_ACCOUNT_PREFIX + merchantId, Direction.CREDIT, request.getAmount(), now, actor));
        iLedgerEntryRepository.save(buildLedgerEntry(transactionId,
                PLATFORM_ACCOUNT, Direction.DEBIT, request.getAmount(), now, actor));

        return CreateTransactionResModel.builder()
                .id(transactionId)
                .status(transaction.getStatus())
                .build();
    }

    private CreateTransactionResModel readCachedResult(String cacheKey) {
        try {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, CreateTransactionResModel.class);
        } catch (JsonProcessingException | RuntimeException ex) {
            log.warn("Idempotency cache read failed for {}, processing as a new request", cacheKey, ex);
            return null;
        }
    }

    private void scheduleCacheWrite(String cacheKey, CreateTransactionResModel result) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheResult(cacheKey, result);
                }
            });
        } else {
            cacheResult(cacheKey, result);
        }
    }

    private void cacheResult(String cacheKey, CreateTransactionResModel result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(cacheKey, json, Duration.ofHours(idempotencyTtlHours));
        } catch (JsonProcessingException | RuntimeException ex) {
            log.warn("Idempotency cache write failed for {}", cacheKey, ex);
        }
    }

    private LedgerEntries buildLedgerEntry(UUID transactionId, String account, Direction direction,
            BigDecimal amount, LocalDateTime now, String actor) {
        LedgerEntries entry = new LedgerEntries();
        entry.setId(UUID.randomUUID());
        entry.setTransactionId(transactionId);
        entry.setAccount(account);
        entry.setDirection(direction);
        entry.setAmount(amount);
        entry.setCreatedAt(now);
        entry.setCreatedBy(actor);
        entry.setIsDeleted(false);
        return entry;
    }
}
