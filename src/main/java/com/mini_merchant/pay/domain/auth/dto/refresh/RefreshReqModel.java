package com.mini_merchant.pay.domain.auth.dto.refresh;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshReqModel {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
