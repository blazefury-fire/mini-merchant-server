package com.mini_merchant.pay.domain.merchants.dto.create;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateMerchantsResModel {
    private UUID id;
}
