package com.mini_merchant.pay.domain.auth.dto.login;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResModel {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
}
