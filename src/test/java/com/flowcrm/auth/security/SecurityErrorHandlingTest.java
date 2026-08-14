package com.flowcrm.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityErrorHandlingTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());


    @BeforeEach
    void setUp() {
        SecurityConfig securityConfig = new SecurityConfig(null, null, objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(new SecureTestController())
                .setControllerAdvice(new com.flowcrm.common.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Custom AuthenticationEntryPoint should format 401 response as ErrorResponse JSON")
    void customAuthenticationEntryPoint_FormatsJsonResponse() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig(null, null, objectMapper);
        var entryPoint = securityConfig.customAuthenticationEntryPoint();

        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/leads");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("Full authentication is required"));

        org.junit.jupiter.api.Assertions.assertEquals(401, response.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        org.junit.jupiter.api.Assertions.assertTrue(response.getContentAsString().contains("\"status\":401"));
        org.junit.jupiter.api.Assertions.assertTrue(response.getContentAsString().contains("\"path\":\"/api/v1/leads\""));
    }

    @Test
    @DisplayName("Custom AccessDeniedHandler should format 403 response as ErrorResponse JSON")
    void customAccessDeniedHandler_FormatsJsonResponse() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig(null, null, objectMapper);
        var accessDeniedHandler = securityConfig.customAccessDeniedHandler();

        var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/v1/users");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access denied"));

        org.junit.jupiter.api.Assertions.assertEquals(403, response.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        org.junit.jupiter.api.Assertions.assertTrue(response.getContentAsString().contains("\"status\":403"));
        org.junit.jupiter.api.Assertions.assertTrue(response.getContentAsString().contains("\"path\":\"/api/v1/users\""));
    }

    @RestController
    @RequestMapping("/test/secure")
    static class SecureTestController {

        @GetMapping("/resource")
        public String secureEndpoint() {
            return "OK";
        }
    }
}
