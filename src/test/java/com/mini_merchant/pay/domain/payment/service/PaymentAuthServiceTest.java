package com.mini_merchant.pay.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mini_merchant.pay.common.exception.UnauthorizedException;
import com.mini_merchant.pay.entity.Merchants;
import com.mini_merchant.pay.repository.merchants.IMerchantRepository;

@ExtendWith(MockitoExtension.class)
class PaymentAuthServiceTest {

    @Mock
    private IMerchantRepository iMerchantRepository;

    @InjectMocks
    private PaymentAuthService paymentAuthService;

    private static final String API_KEY = "test-api-key";
    private static final String SECRET = "test-secret";
    private static final String METHOD = "GET";
    private static final String PATH = "/api/v1/payments/ping";

    private Merchants buildMerchant() {
        Merchants merchant = new Merchants();
        merchant.setId(UUID.randomUUID());
        merchant.setApiKey(API_KEY);
        merchant.setSecret(SECRET);
        merchant.setIsDeleted(false);
        return merchant;
    }

    private String sign(String timestamp) throws Exception {
        String canonical = API_KEY + "\n" + timestamp + "\n" + METHOD + "\n" + PATH;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }

    @Test
    void authenticate_validSignature_returnsMerchant() throws Exception {
        Merchants merchant = buildMerchant();
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String signature = sign(ts);
        when(iMerchantRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(merchant));

        Merchants result = paymentAuthService.authenticate(API_KEY, signature, ts, METHOD, PATH);

        assertThat(result.getId()).isEqualTo(merchant.getId());
    }

    @Test
    void authenticate_missingHeaders_throwsUnauthorized() {
        String ts = String.valueOf(Instant.now().getEpochSecond());

        assertThatThrownBy(() -> paymentAuthService.authenticate(null, "sig", ts, METHOD, PATH))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void authenticate_expiredTimestamp_throwsUnauthorized() {
        String oldTs = String.valueOf(Instant.now().getEpochSecond() - 3600);

        assertThatThrownBy(() -> paymentAuthService.authenticate(API_KEY, "sig", oldTs, METHOD, PATH))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void authenticate_unknownApiKey_throwsUnauthorized() {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        when(iMerchantRepository.findByApiKey(API_KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentAuthService.authenticate(API_KEY, "sig", ts, METHOD, PATH))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void authenticate_badSignature_throwsUnauthorized() {
        Merchants merchant = buildMerchant();
        String ts = String.valueOf(Instant.now().getEpochSecond());
        when(iMerchantRepository.findByApiKey(API_KEY)).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> paymentAuthService.authenticate(API_KEY, "deadbeef", ts, METHOD, PATH))
                .isInstanceOf(UnauthorizedException.class);
    }
}
