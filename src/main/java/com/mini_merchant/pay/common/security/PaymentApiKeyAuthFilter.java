package com.mini_merchant.pay.common.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mini_merchant.pay.common.constant.ApiPath;
import com.mini_merchant.pay.common.constant.HttpStatusCode;
import com.mini_merchant.pay.common.dto.ApiResponse;
import com.mini_merchant.pay.common.exception.UnauthorizedException;
import com.mini_merchant.pay.domain.payment.service.IPaymentAuthService;
import com.mini_merchant.pay.entity.Merchants;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String MERCHANT_ID_ATTRIBUTE = "paymentMerchantId";

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";

    private final IPaymentAuthService iPaymentAuthService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ApiPath.PAYMENTS);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            Merchants merchant = iPaymentAuthService.authenticate(
                    request.getHeader(API_KEY_HEADER),
                    request.getHeader(SIGNATURE_HEADER),
                    request.getHeader(TIMESTAMP_HEADER),
                    request.getMethod(),
                    request.getRequestURI());

            request.setAttribute(MERCHANT_ID_ATTRIBUTE, merchant.getId());
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException ex) {
            writeUnauthorized(response, ex.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatusCode.UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.error(HttpStatusCode.UNAUTHORIZED, message));
    }
}
