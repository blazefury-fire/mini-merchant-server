package com.mini_merchant.pay.domain.ping.service;

import org.springframework.stereotype.Service;

import com.mini_merchant.pay.domain.ping.dto.PingResponse;

@Service
public class PingService {

    public PingResponse ping() {
        return new PingResponse("pong");
    }
}
