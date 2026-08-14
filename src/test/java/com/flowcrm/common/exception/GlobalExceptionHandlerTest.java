package com.flowcrm.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("ResourceNotFoundException should return HTTP 404 with error details")
    void handleResourceNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Lead not found with id 123"))
                .andExpect(jsonPath("$.path").value("/test/not-found"));
    }

    @Test
    @DisplayName("IllegalArgumentException should return HTTP 400")
    void handleIllegalArgument_Returns400() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid parameter supplied"))
                .andExpect(jsonPath("$.path").value("/test/illegal-argument"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException should return HTTP 400 with field errors")
    void handleMethodArgumentNotValid_Returns400WithErrors() throws Exception {
        mockMvc.perform(post("/test/validation-failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/test/validation-failure"))
                .andExpect(jsonPath("$.errors.email").value("must be a valid email"));
    }


    @Test
    @DisplayName("ConstraintViolationException should return HTTP 400 with violation errors")
    void handleConstraintViolation_Returns400() throws Exception {
        mockMvc.perform(get("/test/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/test/constraint-violation"))
                .andExpect(jsonPath("$.errors.phone").value("must match pattern"));
    }

    @Test
    @DisplayName("HttpMessageNotReadableException (malformed JSON) should return HTTP 400")
    void handleHttpMessageNotReadable_Returns400() throws Exception {
        mockMvc.perform(post("/test/malformed-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid_json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request or invalid request body"))
                .andExpect(jsonPath("$.path").value("/test/malformed-json"));
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException should return HTTP 400")
    void handleTypeMismatch_Returns400() throws Exception {
        mockMvc.perform(get("/test/type-mismatch"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Parameter 'id' should be of type UUID"))
                .andExpect(jsonPath("$.path").value("/test/type-mismatch"));
    }

    @Test
    @DisplayName("InvalidTokenException & BadCredentialsException should return HTTP 401")
    void handleAuthenticationExceptions_Returns401() throws Exception {
        mockMvc.perform(get("/test/invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("JWT token expired"))
                .andExpect(jsonPath("$.path").value("/test/invalid-token"));

        mockMvc.perform(get("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.path").value("/test/bad-credentials"));
    }

    @Test
    @DisplayName("AccessDeniedException should return HTTP 403")
    void handleAccessDenied_Returns403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/test/access-denied"));
    }

    @Test
    @DisplayName("DataIntegrityViolation & EmailAlreadyExists should return HTTP 409")
    void handleConflictExceptions_Returns409() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Resource already exists or database constraint violation"))
                .andExpect(jsonPath("$.path").value("/test/data-integrity"));

        mockMvc.perform(get("/test/email-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email user@example.com already exists"))
                .andExpect(jsonPath("$.path").value("/test/email-exists"));
    }

    @Test
    @DisplayName("RateLimitExceededException should return HTTP 429 with Retry-After header")
    void handleRateLimitExceeded_Returns429WithRetryHeader() throws Exception {
        mockMvc.perform(get("/test/rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."))
                .andExpect(jsonPath("$.path").value("/test/rate-limit"));
    }

    @Test
    @DisplayName("Unhandled Exception should return HTTP 500 sanitized without stack traces")
    void handleGenericException_Returns500Sanitized() throws Exception {
        mockMvc.perform(get("/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/test/unexpected-error"));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Lead not found with id 123");
        }

        @GetMapping("/illegal-argument")
        public void throwIllegalArgument() {
            throw new IllegalArgumentException("Invalid parameter supplied");
        }

        public record SampleRequest(
                @jakarta.validation.constraints.Email(message = "must be a valid email")
                String email
        ) {}

        @PostMapping("/validation-failure")
        public void throwValidation(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody SampleRequest request) {
        }


        @GetMapping("/constraint-violation")
        public void throwConstraintViolation() {
            Path path = mock(Path.class);
            when(path.toString()).thenReturn("createLead.phone");
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("must match pattern");
            throw new ConstraintViolationException(Set.of(violation));
        }

        @PostMapping("/malformed-json")
        public void throwMalformedJson() {
            throw new HttpMessageNotReadableException("Required request body is missing", (org.springframework.http.HttpInputMessage) null);
        }

        @GetMapping("/type-mismatch")
        public void throwTypeMismatch() {
            throw new MethodArgumentTypeMismatchException("abc", java.util.UUID.class, "id", null, null);
        }

        @GetMapping("/invalid-token")
        public void throwInvalidToken() {
            throw new InvalidTokenException("JWT token expired");
        }

        @GetMapping("/bad-credentials")
        public void throwBadCredentials() {
            throw new BadCredentialsException("Bad credentials");
        }

        @GetMapping("/access-denied")
        public void throwAccessDenied() {
            throw new AccessDeniedException("Access denied");
        }

        @GetMapping("/data-integrity")
        public void throwDataIntegrity() {
            throw new DataIntegrityViolationException("Unique constraint violation");
        }

        @GetMapping("/email-exists")
        public void throwEmailExists() {
            throw new EmailAlreadyExistsException("Email user@example.com already exists");
        }

        @GetMapping("/rate-limit")
        public void throwRateLimit() {
            throw new RateLimitExceededException("Too many requests. Please try again later.", 30L);
        }

        @GetMapping("/unexpected-error")
        public void throwUnexpected() {
            throw new RuntimeException("Sensitive internal database connection error: password=secret");
        }
    }
}
