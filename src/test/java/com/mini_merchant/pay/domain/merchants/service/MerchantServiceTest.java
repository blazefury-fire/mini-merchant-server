package com.mini_merchant.pay.domain.merchants.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mini_merchant.pay.domain.merchants.dto.create.CreateMerchantsReqModel;
import com.mini_merchant.pay.domain.merchants.dto.create.CreateMerchantsResModel;
import com.mini_merchant.pay.domain.merchants.dto.detail.GetMerchantResModel;
import com.mini_merchant.pay.domain.merchants.dto.update.UpdateMerchantReqModel;
import com.mini_merchant.pay.domain.merchants.service.MerchantService;
import com.mini_merchant.pay.entity.Merchants;
import com.mini_merchant.pay.repository.merchants.IMerchantRepository;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTest {

    @Mock
    private IMerchantRepository iMerchantRepository;

    @InjectMocks
    private MerchantService merchantService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Merchants buildMerchant(boolean isDeleted) {
        Merchants m = new Merchants();
        m.setId(UUID.randomUUID());
        m.setName("Test Merchant");
        m.setEmail("test@example.com");
        m.setApiKey("someApiKey");
        m.setSecret("SECRET");
        m.setStatus("ACTIVE");
        m.setCreatedAt(LocalDateTime.now());
        m.setCreatedBy("admin");
        m.setIsDeleted(isDeleted);
        return m;
    }

    private CreateMerchantsReqModel buildCreateReq() {
        CreateMerchantsReqModel req = new CreateMerchantsReqModel();
        req.setName("Test Merchant");
        req.setEmail("test@example.com");
        req.setStatus("ACTIVE");
        req.setCreatedBy("admin");
        return req;
    }

    private UpdateMerchantReqModel buildUpdateReq() {
        UpdateMerchantReqModel req = new UpdateMerchantReqModel();
        req.setName("Updated Name");
        req.setEmail("updated@example.com");
        req.setStatus("INACTIVE");
        req.setUpdatedBy("admin");
        return req;
    }

    // ── createMerchant ───────────────────────────────────────────────────────

    @Test
    void createMerchant_success() {
        when(iMerchantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateMerchantsResModel result = merchantService.createMerchant(buildCreateReq());

        ArgumentCaptor<Merchants> captor = ArgumentCaptor.forClass(Merchants.class);
        verify(iMerchantRepository, times(1)).save(captor.capture());

        Merchants saved = captor.getValue();
        assertThat(result.getId()).isNotNull();
        assertThat(saved.getIsDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getApiKey()).isNotBlank();
        assertThat(saved.getSecret()).isNotBlank();
    }

    // ── getMerchantById ──────────────────────────────────────────────────────

    @Test
    void getMerchantById_success() {
        Merchants merchant = buildMerchant(false);
        when(iMerchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));

        GetMerchantResModel result = merchantService.getMerchantById(merchant.getId());

        assertThat(result.getId()).isEqualTo(merchant.getId());
        assertThat(result.getName()).isEqualTo(merchant.getName());
        assertThat(result.getEmail()).isEqualTo(merchant.getEmail());
        assertThat(result.getStatus()).isEqualTo(merchant.getStatus());
    }

    @Test
    void getMerchantById_notFound() {
        UUID id = UUID.randomUUID();
        when(iMerchantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.getMerchantById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getMerchantById_alreadyDeleted() {
        Merchants merchant = buildMerchant(true);
        when(iMerchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> merchantService.getMerchantById(merchant.getId()))
                .isInstanceOf(RuntimeException.class);
    }

    // ── updateMerchant ───────────────────────────────────────────────────────

    @Test
    void updateMerchant_success() {
        Merchants merchant = buildMerchant(false);
        when(iMerchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));
        when(iMerchantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GetMerchantResModel result = merchantService.updateMerchant(merchant.getId(), buildUpdateReq());

        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        assertThat(result.getStatus()).isEqualTo("INACTIVE");
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(iMerchantRepository, times(1)).save(any());
    }

    @Test
    void updateMerchant_notFound() {
        UUID id = UUID.randomUUID();
        when(iMerchantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.updateMerchant(id, buildUpdateReq()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateMerchant_alreadyDeleted() {
        Merchants merchant = buildMerchant(true);
        when(iMerchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> merchantService.updateMerchant(merchant.getId(), buildUpdateReq()))
                .isInstanceOf(RuntimeException.class);
    }

    // ── deleteMerchant ───────────────────────────────────────────────────────

    @Test
    void deleteMerchant_success() {
        Merchants merchant = buildMerchant(false);
        when(iMerchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));
        when(iMerchantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        merchantService.deleteMerchant(merchant.getId());

        ArgumentCaptor<Merchants> captor = ArgumentCaptor.forClass(Merchants.class);
        verify(iMerchantRepository, times(1)).save(captor.capture());

        Merchants saved = captor.getValue();
        assertThat(saved.getIsDeleted()).isTrue();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void deleteMerchant_notFound() {
        UUID id = UUID.randomUUID();
        when(iMerchantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.deleteMerchant(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void deleteMerchant_alreadyDeleted() {
        Merchants merchant = buildMerchant(true);
        when(iMerchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> merchantService.deleteMerchant(merchant.getId()))
                .isInstanceOf(RuntimeException.class);
    }

    // ── getMerchants ─────────────────────────────────────────────────────────

    @Test
    void getMerchants_returnsList() {
        Merchants m1 = buildMerchant(false);
        Merchants m2 = buildMerchant(false);
        when(iMerchantRepository.findAllActive()).thenReturn(List.of(m1, m2));

        List<GetMerchantResModel> result = merchantService.getMerchants();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(m1.getId());
        assertThat(result.get(1).getId()).isEqualTo(m2.getId());
    }

    @Test
    void getMerchants_emptyList() {
        when(iMerchantRepository.findAllActive()).thenReturn(List.of());

        List<GetMerchantResModel> result = merchantService.getMerchants();

        assertThat(result).isEmpty();
    }
}
