package com.mini_merchant.pay.domain.ping.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mini_merchant.pay.common.constant.ApiPath;
import com.mini_merchant.pay.domain.ping.dto.PingResponse;
import com.mini_merchant.pay.domain.ping.service.PingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PingController {

    private final PingService pingService;

    @GetMapping(ApiPath.PING)
    public PingResponse ping() {
        return pingService.ping();
    }
}
