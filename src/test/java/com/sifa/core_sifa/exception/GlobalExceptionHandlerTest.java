package com.sifa.core_sifa.exception;

import com.sifa.core_sifa.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/core/api/v1/test");
    }

    @Test
    void handleResourceNotFoundException_returns404() {
        var ex = new ResourceNotFoundException("Recurso no encontrado");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Recurso no encontrado");
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getPath()).isEqualTo("/core/api/v1/test");
    }

    @Test
    void handleIllegalArgumentException_returns400() {
        var ex = new IllegalArgumentException("Argumento inválido");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Argumento inválido");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void handleIllegalStateException_returns409() {
        var ex = new IllegalStateException("Estado inválido");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalStateException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Estado inválido");
        assertThat(response.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    void handleValidationExceptions_returns400WithFirstError() {
        var ex = org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
        var bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "campo", "El campo es obligatorio"));
        org.mockito.BDDMockito.given(ex.getBindingResult()).willReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("El campo es obligatorio");
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }
}
