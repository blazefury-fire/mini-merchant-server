package com.mini_merchant.pay.domain.merchants.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mini_merchant.pay.common.dto.ApiResponse;
import com.mini_merchant.pay.domain.merchants.dto.create.CreateMerchantsReqModel;
import com.mini_merchant.pay.domain.merchants.dto.create.CreateMerchantsResModel;
import com.mini_merchant.pay.domain.merchants.dto.detail.GetMerchantResModel;
import com.mini_merchant.pay.domain.merchants.dto.update.UpdateMerchantReqModel;
import com.mini_merchant.pay.domain.merchants.service.IMerchantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {
    private final IMerchantService iMerchantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateMerchantsResModel> create(@Valid @RequestBody CreateMerchantsReqModel request) {
        return ApiResponse.created(iMerchantService.createMerchant(request));
    }

    @GetMapping
    public ApiResponse<List<GetMerchantResModel>> getAll() {
        return ApiResponse.success(iMerchantService.getMerchants());
    }

    @GetMapping("/{id}")
    public ApiResponse<GetMerchantResModel> getById(@PathVariable UUID id) {
        return ApiResponse.success(iMerchantService.getMerchantById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<GetMerchantResModel> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateMerchantReqModel request) {
        return ApiResponse.success(iMerchantService.updateMerchant(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        iMerchantService.deleteMerchant(id);
        return ApiResponse.success(null);
    }
}
