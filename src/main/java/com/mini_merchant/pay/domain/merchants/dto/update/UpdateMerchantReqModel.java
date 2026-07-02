package com.mini_merchant.pay.domain.merchants.dto.update;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMerchantReqModel {
    @NotBlank(message = "Merchant name is required")
    private String name;

    @NotBlank(message = "Merchant email is required")
    @Email(message = "Merchant email must be a valid email address")
    private String email;

    @NotBlank(message = "Merchant status is required")
    private String status;

    @NotBlank(message = "updatedBy is required")
    private String updatedBy;
}
