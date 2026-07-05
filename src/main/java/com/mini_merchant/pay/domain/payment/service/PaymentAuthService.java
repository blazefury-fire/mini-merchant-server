package com.mini_merchant.pay.domain.payment.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.mini_merchant.pay.common.exception.UnauthorizedException;
import com.mini_merchant.pay.entity.Merchants;
import com.mini_merchant.pay.repository.merchants.IMerchantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentAuthService implements IPaymentAuthService {
    private final IMerchantRepository iMerchantRepository;

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final long MAX_SKEW_SECONDS = 300;

    @Override
    public Merchants authenticate(String apiKey, String signature, String timestamp, String method, String path) {
        if (isBlank(apiKey) || isBlank(signature) || isBlank(timestamp)) {
            throw new UnauthorizedException("Missing required authentication headers");
        }

        validateTimestamp(timestamp);

        Merchants merchant = iMerchantRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new UnauthorizedException("Invalid API credentials"));

        String canonical = apiKey + "\n" + timestamp + "\n" + method + "\n" + path;
        String expected = hmacSha256Hex(merchant.getSecret(), canonical);

        boolean matches = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw new UnauthorizedException("Invalid API credentials");
        }

        return merchant;
    }

    private void validateTimestamp(String timestamp) {
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("Invalid timestamp");
        }

        if (Math.abs(Instant.now().getEpochSecond() - ts) > MAX_SKEW_SECONDS) {
            throw new UnauthorizedException("Request timestamp outside acceptable window");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Failed to compute HMAC signature", ex);
        }
    }
}
