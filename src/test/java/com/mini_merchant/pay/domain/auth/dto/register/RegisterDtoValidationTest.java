package com.mini_merchant.pay.domain.auth.dto.register;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RegisterDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private RegisterReqModel build(String email, String password) {
        RegisterReqModel req = new RegisterReqModel();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private boolean hasViolation(Set<ConstraintViolation<RegisterReqModel>> violations, String field) {
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Test
    void password_strong_noViolation() {
        Set<ConstraintViolation<RegisterReqModel>> violations =
                validator.validate(build("user@example.com", "Passw0rd!"));

        assertThat(hasViolation(violations, "password")).isFalse();
    }

    @Test
    void password_missingUppercase_hasViolation() {
        Set<ConstraintViolation<RegisterReqModel>> violations =
                validator.validate(build("user@example.com", "passw0rd!"));

        assertThat(hasViolation(violations, "password")).isTrue();
    }

    @Test
    void password_missingSpecial_hasViolation() {
        Set<ConstraintViolation<RegisterReqModel>> violations =
                validator.validate(build("user@example.com", "Password123"));

        assertThat(hasViolation(violations, "password")).isTrue();
    }

    @Test
    void password_missingDigit_hasViolation() {
        Set<ConstraintViolation<RegisterReqModel>> violations =
                validator.validate(build("user@example.com", "Password!"));

        assertThat(hasViolation(violations, "password")).isTrue();
    }

    @Test
    void password_tooShort_hasViolation() {
        Set<ConstraintViolation<RegisterReqModel>> violations =
                validator.validate(build("user@example.com", "Pw1!"));

        assertThat(hasViolation(violations, "password")).isTrue();
    }

    @Test
    void email_invalid_hasViolation() {
        Set<ConstraintViolation<RegisterReqModel>> violations =
                validator.validate(build("not-an-email", "Passw0rd!"));

        assertThat(hasViolation(violations, "email")).isTrue();
    }
}
