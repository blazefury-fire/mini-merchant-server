package com.mini_merchant.pay.ping.service;

import org.springframework.stereotype.Service;

import com.mini_merchant.pay.ping.dto.PingResponse;

@Service
public class PingService {

    public PingResponse ping() {
        return new PingResponse("pong");
    }
}
