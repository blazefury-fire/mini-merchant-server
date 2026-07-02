package com.mini_merchant.pay.domain.merchants.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mini_merchant.pay.domain.merchants.dto.create.CreateMerchantsReqModel;
import com.mini_merchant.pay.domain.merchants.dto.update.UpdateMerchantReqModel;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class MerchantDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CreateMerchantsReqModel buildCreate(String email) {
        CreateMerchantsReqModel req = new CreateMerchantsReqModel();
        req.setName("Acme");
        req.setEmail(email);
        req.setStatus("ACTIVE");
        req.setCreatedBy("admin");
        return req;
    }

    private UpdateMerchantReqModel buildUpdate(String email) {
        UpdateMerchantReqModel req = new UpdateMerchantReqModel();
        req.setName("Acme");
        req.setEmail(email);
        req.setStatus("ACTIVE");
        req.setUpdatedBy("admin");
        return req;
    }

    @Test
    void create_invalidEmail_hasViolation() {
        Set<ConstraintViolation<CreateMerchantsReqModel>> violations =
                validator.validate(buildCreate("not-an-email"));

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void create_validEmail_noEmailViolation() {
        Set<ConstraintViolation<CreateMerchantsReqModel>> violations =
                validator.validate(buildCreate("valid@example.com1"));

        assertThat(violations)
                .noneMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    void update_invalidEmail_hasViolation() {
        Set<ConstraintViolation<UpdateMerchantReqModel>> violations =
                validator.validate(buildUpdate("bad@"));

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }
}
