package com.mini_merchant.pay.domain.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mini_merchant.pay.common.constant.ApiPath;
import com.mini_merchant.pay.common.dto.ApiResponse;
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
}
