package com.mini_merchant.pay.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.mini_merchant.pay.common.constant.Direction;
import com.mini_merchant.pay.common.constant.TransactionStatus;
import com.mini_merchant.pay.domain.payment.dto.transaction.CreateTransactionReqModel;
import com.mini_merchant.pay.domain.payment.dto.transaction.CreateTransactionResModel;
import com.mini_merchant.pay.entity.LedgerEntries;
import com.mini_merchant.pay.entity.Transactions;
import com.mini_merchant.pay.repository.ledgerentries.ILedgerEntryRepository;
import com.mini_merchant.pay.repository.transactions.ITransactionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private ITransactionRepository iTransactionRepository;

    @Mock
    private ILedgerEntryRepository iLedgerEntryRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = spy(new TransactionService(
                iTransactionRepository, iLedgerEntryRepository, stringRedisTemplate, objectMapper));
        ReflectionTestUtils.setField(transactionService, "idempotencyTtlHours", 24L);
    }

    private CreateTransactionReqModel buildReq() {
        CreateTransactionReqModel req = new CreateTransactionReqModel();
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("VND");
        return req;
    }

    @Test
    void createTransaction_cacheMiss_coreLogicSuccess_persistsSucceededTxWithLedger() {
        UUID merchantId = UUID.randomUUID();
        String idempotencyKey = "idem-123";
        String cacheKey = "idempotency:transaction:" + merchantId + ":" + idempotencyKey;
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(iTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(iLedgerEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doReturn(3).when(transactionService).runCoreLogic();

        CreateTransactionResModel result =
                transactionService.createTransaction(merchantId, idempotencyKey, buildReq());

        ArgumentCaptor<Transactions> txCaptor = ArgumentCaptor.forClass(Transactions.class);
        verify(iTransactionRepository, times(2)).save(txCaptor.capture());
        Transactions savedTx = txCaptor.getValue();
        assertThat(savedTx.getMerchantId()).isEqualTo(merchantId);
        assertThat(savedTx.getAmount()).isEqualByComparingTo("100.00");
        assertThat(savedTx.getCurrency()).isEqualTo("VND");
        assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.SUCCESS.name());
        assertThat(savedTx.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(savedTx.getIsDeleted()).isFalse();
        assertThat(savedTx.getCreatedAt()).isNotNull();
        assertThat(savedTx.getCreatedBy()).isEqualTo(merchantId.toString());
        assertThat(savedTx.getUpdatedAt()).isNotNull();
        assertThat(savedTx.getUpdatedBy()).isEqualTo(merchantId.toString());

        ArgumentCaptor<LedgerEntries> entryCaptor = ArgumentCaptor.forClass(LedgerEntries.class);
        verify(iLedgerEntryRepository, times(2)).save(entryCaptor.capture());
        List<LedgerEntries> entries = entryCaptor.getAllValues();
        assertThat(entries).hasSize(2);
        assertThat(entries).allSatisfy(e -> {
            assertThat(e.getTransactionId()).isEqualTo(savedTx.getId());
            assertThat(e.getAmount()).isEqualByComparingTo("100.00");
            assertThat(e.getIsDeleted()).isFalse();
            assertThat(e.getCreatedAt()).isNotNull();
        });
        assertThat(entries).anyMatch(
                e -> e.getDirection() == Direction.CREDIT && e.getAccount().equals("MERCHANT:" + merchantId));
        assertThat(entries).anyMatch(
                e -> e.getDirection() == Direction.DEBIT && e.getAccount().equals("PLATFORM"));

        verify(valueOperations).set(eq(cacheKey), anyString(), any(Duration.class));
        assertThat(result.getId()).isEqualTo(savedTx.getId());
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS.name());
    }

    @Test
    void createTransaction_cacheMiss_coreLogicFailure_marksFailedAndWritesNoLedger() {
        UUID merchantId = UUID.randomUUID();
        String idempotencyKey = "idem-456";
        String cacheKey = "idempotency:transaction:" + merchantId + ":" + idempotencyKey;
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(iTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doReturn(0).when(transactionService).runCoreLogic();

        CreateTransactionResModel result =
                transactionService.createTransaction(merchantId, idempotencyKey, buildReq());

        ArgumentCaptor<Transactions> txCaptor = ArgumentCaptor.forClass(Transactions.class);
        verify(iTransactionRepository, times(2)).save(txCaptor.capture());
        Transactions savedTx = txCaptor.getValue();
        assertThat(savedTx.getStatus()).isEqualTo(TransactionStatus.FAILED.name());
        assertThat(savedTx.getUpdatedAt()).isNotNull();
        assertThat(savedTx.getUpdatedBy()).isEqualTo(merchantId.toString());

        verify(iLedgerEntryRepository, never()).save(any());
        verify(valueOperations).set(eq(cacheKey), anyString(), any(Duration.class));
        assertThat(result.getId()).isEqualTo(savedTx.getId());
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED.name());
    }

    @Test
    void createTransaction_cacheHit_returnsCachedResultAndSkipsDb() throws Exception {
        UUID merchantId = UUID.randomUUID();
        String idempotencyKey = "idem-123";
        String cacheKey = "idempotency:transaction:" + merchantId + ":" + idempotencyKey;
        UUID priorId = UUID.randomUUID();
        CreateTransactionResModel prior = CreateTransactionResModel.builder()
                .id(priorId)
                .status(TransactionStatus.SUCCESS.name())
                .build();
        String cachedJson = objectMapper.writeValueAsString(prior);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(cachedJson);

        CreateTransactionResModel result =
                transactionService.createTransaction(merchantId, idempotencyKey, buildReq());

        assertThat(result.getId()).isEqualTo(priorId);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS.name());
        verify(iTransactionRepository, never()).save(any());
        verify(iLedgerEntryRepository, never()).save(any());
    }
}
