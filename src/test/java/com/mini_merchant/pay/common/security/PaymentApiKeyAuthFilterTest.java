package com.mini_merchant.pay.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.mini_merchant.pay.common.constant.HttpStatusCode;
import com.mini_merchant.pay.common.ratelimit.IRateLimiterService;
import com.mini_merchant.pay.domain.payment.service.IPaymentAuthService;
import com.mini_merchant.pay.entity.Merchants;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class PaymentApiKeyAuthFilterTest {

    @Mock
    private IPaymentAuthService iPaymentAuthService;

    @Mock
    private IRateLimiterService iRateLimiterService;

    @Mock
    private FilterChain filterChain;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Merchants merchant() {
        Merchants m = new Merchants();
        m.setId(UUID.randomUUID());
        return m;
    }

    private MockHttpServletRequest paymentRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/payments/ping");
        return request;
    }

    @Test
    void underLimit_passesThrough() throws Exception {
        PaymentApiKeyAuthFilter filter =
                new PaymentApiKeyAuthFilter(iPaymentAuthService, iRateLimiterService, objectMapper);
        Merchants merchant = merchant();
        when(iPaymentAuthService.authenticate(any(), any(), any(), any(), any())).thenReturn(merchant);
        when(iRateLimiterService.tryAcquire(merchant.getId())).thenReturn(true);

        MockHttpServletRequest request = paymentRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(request.getAttribute(PaymentApiKeyAuthFilter.MERCHANT_ID_ATTRIBUTE))
                .isEqualTo(merchant.getId());
    }

    @Test
    void overLimit_returns429AndStopsChain() throws Exception {
        PaymentApiKeyAuthFilter filter =
                new PaymentApiKeyAuthFilter(iPaymentAuthService, iRateLimiterService, objectMapper);
        Merchants merchant = merchant();
        when(iPaymentAuthService.authenticate(any(), any(), any(), any(), any())).thenReturn(merchant);
        when(iRateLimiterService.tryAcquire(merchant.getId())).thenReturn(false);

        MockHttpServletRequest request = paymentRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.TOO_MANY_REQUESTS);
        assertThat(response.getContentAsString()).contains("429");
        verify(filterChain, never()).doFilter(any(), any());
    }
}
