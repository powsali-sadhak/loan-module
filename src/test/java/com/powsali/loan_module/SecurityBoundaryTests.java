package com.powsali.loan_module;

import com.powsali.loan_module.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-demo")
class SecurityBoundaryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Test
    void protectedEndpointsRejectRequestsWithoutDemoIdentity() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void demoDirectoryIsAvailableForTheLocalUserSwitcher() throws Exception {
        mockMvc.perform(get("/api/demo/users"))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotAccessSupportStaffQueue() throws Exception {
        Long employeeId = users.findByUsername("employee1")
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/support/tickets/queue")
                        .header("X-Demo-User-Id", employeeId))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotAccessAdminWorkflowConfiguration() throws Exception {
        Long employeeId = users.findByUsername("employee1")
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/admin/workflows")
                        .header("X-Demo-User-Id", employeeId))
                .andExpect(status().isForbidden());
    }

    @Test
    void selectedEmployeeCanAccessTheirNotificationInbox() throws Exception {
        Long employeeId = users.findByUsername("employee1")
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/notifications")
                        .header("X-Demo-User-Id", employeeId))
                .andExpect(status().isOk());
    }
}
