package com.mini_merchant.pay.domain.merchants.dto.detail;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetMerchantResModel {
    private UUID id;
    private String name;
    private String email;
    private String status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private Boolean isDeleted;
}
