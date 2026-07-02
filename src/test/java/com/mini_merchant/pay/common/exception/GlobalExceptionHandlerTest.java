package com.mini_merchant.pay.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.mini_merchant.pay.common.constant.HttpStatusCode;
import com.mini_merchant.pay.common.dto.ApiResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404() {
        ApiResponse<Void> res = handler.handleNotFound(new NotFoundException("Merchant not found: abc"));

        assertThat(res.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND);
        assertThat(res.getMessage()).contains("Merchant not found");
        assertThat(res.getData()).isNull();
    }

    @Test
    void handleValidation_returns400WithFieldMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("createMerchantsReqModel", "email",
                        "Merchant email must be a valid email address")));

        ApiResponse<Void> res = handler.handleValidation(ex);

        assertThat(res.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST);
        assertThat(res.getMessage()).contains("email");
        assertThat(res.getData()).isNull();
    }

    @Test
    void handleGeneric_returns500() {
        ApiResponse<Void> res = handler.handleGeneric(new RuntimeException("boom"));

        assertThat(res.getStatus()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR);
        assertThat(res.getMessage()).isEqualTo("boom");
    }
}
