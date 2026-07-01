package com.mini_merchant.pay.domain.merchants.dto.create;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMerchantsReqModel {
    @NotBlank(message = "Merchant name is required")
    private String name;

    @NotBlank(message = "Merchant email is required")
    private String email;

    @NotBlank(message = "Merchant status is required")
    private String status;

    @NotBlank(message = "Merchant createdBy is required")
    private String createdBy;
}
