package com.mini_merchant.pay.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mini_merchant.pay.common.constant.ApiPath;
import com.mini_merchant.pay.common.dto.ApiResponse;
import com.mini_merchant.pay.domain.auth.dto.login.LoginReqModel;
import com.mini_merchant.pay.domain.auth.dto.login.LoginResModel;
import com.mini_merchant.pay.domain.auth.dto.logout.LogoutReqModel;
import com.mini_merchant.pay.domain.auth.dto.refresh.RefreshReqModel;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterReqModel;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterResModel;
import com.mini_merchant.pay.domain.auth.service.IAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiPath.AUTH)
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService iAuthService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResModel> register(@Valid @RequestBody RegisterReqModel request) {
        return ApiResponse.created(iAuthService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResModel> login(@Valid @RequestBody LoginReqModel request) {
        return ApiResponse.success(iAuthService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResModel> refresh(@Valid @RequestBody RefreshReqModel request) {
        return ApiResponse.success(iAuthService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutReqModel request) {
        iAuthService.logout(request);
        return ApiResponse.success(null);
    }
}
