package com.mini_merchant.pay.domain.merchants.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mini_merchant.pay.common.exception.NotFoundException;
import com.mini_merchant.pay.domain.merchants.dto.create.CreateMerchantsReqModel;
import com.mini_merchant.pay.domain.merchants.dto.create.CreateMerchantsResModel;
import com.mini_merchant.pay.domain.merchants.dto.detail.GetMerchantResModel;
import com.mini_merchant.pay.domain.merchants.dto.update.UpdateMerchantReqModel;
import com.mini_merchant.pay.entity.Merchants;
import com.mini_merchant.pay.repository.merchants.IMerchantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MerchantService implements IMerchantService {
    private final IMerchantRepository iMerchantRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public CreateMerchantsResModel createMerchant(CreateMerchantsReqModel request) {
        Merchants merchant = new Merchants();

        UUID id = UUID.randomUUID();
        merchant.setId(id);
        merchant.setName(request.getName());
        merchant.setEmail(request.getEmail());
        merchant.setApiKey(this.generateApiKey());
        merchant.setSecret(this.generateSecret());
        merchant.setStatus(request.getStatus());
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setCreatedBy(request.getCreatedBy());
        merchant.setIsDeleted(false);

        // save db
        iMerchantRepository.save(merchant);

        return CreateMerchantsResModel.builder().id(id).build();
    }

    public GetMerchantResModel getMerchantById(UUID id) {
        Merchants merchant = iMerchantRepository.findById(id)
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + id));

        return toResModel(merchant);
    }

    @CacheEvict(cacheNames = "merchantByApiKey", allEntries = true)
    @Transactional
    public GetMerchantResModel updateMerchant(UUID id, UpdateMerchantReqModel request) {
        Merchants merchant = iMerchantRepository.findById(id)
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + id));

        merchant.setName(request.getName());
        merchant.setEmail(request.getEmail());
        merchant.setStatus(request.getStatus());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchant.setUpdatedBy(request.getUpdatedBy());

        iMerchantRepository.save(merchant);

        return toResModel(merchant);
    }

    @CacheEvict(cacheNames = "merchantByApiKey", allEntries = true)
    @Transactional
    public void deleteMerchant(UUID id) {
        Merchants merchant = iMerchantRepository.findById(id)
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new NotFoundException("Merchant not found: " + id));

        merchant.setIsDeleted(true);
        merchant.setUpdatedAt(LocalDateTime.now());

        iMerchantRepository.save(merchant);
    }

    public List<GetMerchantResModel> getMerchants() {
        return iMerchantRepository.findAllActive()
                .stream()
                .map(this::toResModel)
                .toList();
    }

    private GetMerchantResModel toResModel(Merchants merchant) {
        return GetMerchantResModel.builder()
                .id(merchant.getId())
                .name(merchant.getName())
                .email(merchant.getEmail())
                .status(merchant.getStatus())
                .createdAt(merchant.getCreatedAt())
                .createdBy(merchant.getCreatedBy())
                .updatedAt(merchant.getUpdatedAt())
                .updatedBy(merchant.getUpdatedBy())
                .isDeleted(merchant.getIsDeleted())
                .build();
    }

    private String generateApiKey() {
        byte[] bytes = new byte[7];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes).toUpperCase();
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}