package com.flowcrm.common.config;

import com.flowcrm.security.RateLimiterInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebMvcConfigTest {

    @Mock
    private RateLimiterInterceptor rateLimiterInterceptor;

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/api/v1/leads")
    static class DummyLeadController {
        @GetMapping
        public String getLeads() {
            return "leads";
        }

        @GetMapping("/{id}")
        public String getLeadById(@PathVariable String id) {
            return "lead-" + id;
        }
    }

    @RestController
    @RequestMapping("/api/v1/tasks")
    static class DummyTaskController {
        @GetMapping
        public String getTasks() {
            return "tasks";
        }

        @GetMapping("/{id}")
        public String getTaskById(@PathVariable String id) {
            return "task-" + id;
        }
    }

    @BeforeEach
    void setUp() {
        when(rateLimiterInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        WebMvcConfig webMvcConfig = new WebMvcConfig(rateLimiterInterceptor);

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyLeadController(), new DummyTaskController())
                .addInterceptors(rateLimiterInterceptor)
                .build();
    }

    @Test
    void testRootPathLeadsTriggersInterceptor() throws Exception {
        mockMvc.perform(get("/api/v1/leads"))
                .andExpect(status().isOk());

        verify(rateLimiterInterceptor, atLeastOnce()).preHandle(any(), any(), any());
    }

    @Test
    void testSubPathLeadsTriggersInterceptor() throws Exception {
        mockMvc.perform(get("/api/v1/leads/123"))
                .andExpect(status().isOk());

        verify(rateLimiterInterceptor, atLeastOnce()).preHandle(any(), any(), any());
    }

    @Test
    void testRootPathTasksTriggersInterceptor() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk());

        verify(rateLimiterInterceptor, atLeastOnce()).preHandle(any(), any(), any());
    }

    @Test
    void testSubPathTasksTriggersInterceptor() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/123"))
                .andExpect(status().isOk());

        verify(rateLimiterInterceptor, atLeastOnce()).preHandle(any(), any(), any());
    }
}
