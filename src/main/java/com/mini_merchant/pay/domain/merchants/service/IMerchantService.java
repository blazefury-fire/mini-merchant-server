package com.mini_merchant.pay.domain.merchants.service;

import java.util.List;
import java.util.UUID;

import com.mini_merchant.pay.domain.merchants.dto.create.CreateMerchantsReqModel;
import com.mini_merchant.pay.domain.merchants.dto.create.CreateMerchantsResModel;
import com.mini_merchant.pay.domain.merchants.dto.detail.GetMerchantResModel;
import com.mini_merchant.pay.domain.merchants.dto.update.UpdateMerchantReqModel;

public interface IMerchantService {
    CreateMerchantsResModel createMerchant(CreateMerchantsReqModel request);

    GetMerchantResModel getMerchantById(UUID id);

    GetMerchantResModel updateMerchant(UUID id, UpdateMerchantReqModel request);

    void deleteMerchant(UUID id);

    List<GetMerchantResModel> getMerchants();
}