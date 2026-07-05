package com.mini_merchant.pay.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.mini_merchant.pay.common.constant.Role;
import com.mini_merchant.pay.entity.Users;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-that-is-definitely-long-enough-32-bytes-1234567890";

    private Users buildUser() {
        Users user = new Users();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(Role.USER);
        return user;
    }

    @Test
    void generateAccessToken_containsExpectedClaims() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 15, 7);
        Users user = buildUser();

        String token = provider.generateAccessToken(user);

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void getAccessTokenTtlSeconds_convertsMinutes() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 15, 7);

        assertThat(provider.getAccessTokenTtlSeconds()).isEqualTo(900L);
    }

    @Test
    void parseToken_validToken_returnsClaims() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 15, 7);
        Users user = buildUser();
        String token = provider.generateAccessToken(user);

        Claims claims = provider.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void parseToken_tamperedToken_throws() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 15, 7);

        assertThatThrownBy(() -> provider.parseToken("not.a.valid.token"))
                .isInstanceOf(JwtException.class);
    }
}
