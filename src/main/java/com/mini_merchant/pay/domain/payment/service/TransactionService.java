package com.mini_merchant.pay.domain.payment.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    private static final Duration CORE_LOGIC_DELAY = Duration.ofSeconds(3);
    private static final int CORE_LOGIC_MAX_INCLUSIVE = 5;
    private static final int CORE_LOGIC_FAILURE_RESULT = 0;

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

        Transactions transaction = createPendingTransaction(merchantId, idempotencyKey, request, now, actor);

        int coreResult = runCoreLogic();

        if (coreResult == CORE_LOGIC_FAILURE_RESULT) {
            finalizeAsFailed(transaction, actor);
        } else {
            finalizeAsSuccess(transaction, merchantId, request, actor);
        }

        return CreateTransactionResModel.builder()
                .id(transaction.getId())
                .status(transaction.getStatus())
                .build();
    }

    private Transactions createPendingTransaction(UUID merchantId, String idempotencyKey,
            CreateTransactionReqModel request, LocalDateTime now, String actor) {
        Transactions transaction = new Transactions();
        transaction.setId(UUID.randomUUID());
        transaction.setMerchantId(merchantId);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus(TransactionStatus.PENDING.name());
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setCreatedAt(now);
        transaction.setCreatedBy(actor);
        transaction.setIsDeleted(false);
        return iTransactionRepository.save(transaction);
    }

    /**
     * Test-only core-logic stub: waits 3 seconds and returns a random result in the
     * inclusive range 0..5. A result of 0 means the transaction fails; 1..5 means it succeeds.
     * Package-private so unit tests can stub it on a spy (avoids the sleep and randomness).
     */
    int runCoreLogic() {
        try {
            Thread.sleep(CORE_LOGIC_DELAY.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Core logic processing was interrupted", ex);
        }
        return ThreadLocalRandom.current().nextInt(0, CORE_LOGIC_MAX_INCLUSIVE + 1);
    }

    private void finalizeAsFailed(Transactions transaction, String actor) {
        applyStatusTransition(transaction, TransactionStatus.FAILED, actor);
        iTransactionRepository.save(transaction);
    }

    private void finalizeAsSuccess(Transactions transaction, UUID merchantId,
            CreateTransactionReqModel request, String actor) {
        applyStatusTransition(transaction, TransactionStatus.SUCCESS, actor);
        iTransactionRepository.save(transaction);

        LocalDateTime now = LocalDateTime.now();
        iLedgerEntryRepository.save(buildLedgerEntry(transaction.getId(),
                MERCHANT_ACCOUNT_PREFIX + merchantId, Direction.CREDIT, request.getAmount(), now, actor));
        iLedgerEntryRepository.save(buildLedgerEntry(transaction.getId(),
                PLATFORM_ACCOUNT, Direction.DEBIT, request.getAmount(), now, actor));
    }

    private void applyStatusTransition(Transactions transaction, TransactionStatus target, String actor) {
        TransactionStatus current = TransactionStatus.valueOf(transaction.getStatus());
        if (!current.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Illegal transaction status transition: " + current + " -> " + target);
        }
        transaction.setStatus(target.name());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction.setUpdatedBy(actor);
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
