package com.mini_merchant.pay.domain.auth.dto.logout;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutReqModel {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
