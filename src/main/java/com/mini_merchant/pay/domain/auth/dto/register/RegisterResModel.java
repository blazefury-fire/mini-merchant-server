package com.mini_merchant.pay.domain.auth.dto.register;

import java.util.UUID;

import com.mini_merchant.pay.common.constant.Role;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterResModel {
    private UUID id;
    private String email;
    private Role role;
}
